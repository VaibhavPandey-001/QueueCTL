package com.queuectl.service;

import com.queuectl.TestBase;
import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.exception.DuplicateJobException;
import com.queuectl.exception.JobNotFoundException;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JobService: enqueue, claim, complete, fail.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobServiceTest extends TestBase {

    private DatabaseManager dbManager;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        dbManager = createTestDatabase();
        jobService = new JobService(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @Order(1)
    void testEnqueueJob() {
        Job job = new Job("svc-1", "echo test");
        Job result = jobService.enqueue(job);

        assertEquals("svc-1", result.getId());
        assertEquals(JobState.PENDING, result.getState());
    }

    @Test
    @Order(2)
    void testEnqueueDuplicateThrows() {
        Job job = new Job("dup-svc", "echo dup");
        jobService.enqueue(job);

        assertThrows(DuplicateJobException.class, () -> jobService.enqueue(new Job("dup-svc", "echo again")));
    }

    @Test
    @Order(3)
    void testEnqueueBlankIdThrows() {
        Job job = new Job("", "echo test");
        assertThrows(IllegalArgumentException.class, () -> jobService.enqueue(job));
    }

    @Test
    @Order(4)
    void testEnqueueBlankCommandThrows() {
        Job job = new Job("no-cmd", "");
        assertThrows(IllegalArgumentException.class, () -> jobService.enqueue(job));
    }

    @Test
    @Order(5)
    void testClaimJob() {
        jobService.enqueue(new Job("claim-svc", "echo claim"));
        Job claimed = jobService.claimJob("worker-1");

        assertNotNull(claimed);
        assertEquals("claim-svc", claimed.getId());
        assertEquals("worker-1", claimed.getWorkerId());
    }

    @Test
    @Order(6)
    void testClaimJobReturnsNullWhenEmpty() {
        Job claimed = jobService.claimJob("worker-1");
        assertNull(claimed);
    }

    @Test
    @Order(7)
    void testCompleteJob() {
        jobService.enqueue(new Job("comp-svc", "echo done"));
        jobService.claimJob("worker-1");
        jobService.completeJob("comp-svc", "done output");

        Job job = jobService.getJob("comp-svc");
        assertEquals(JobState.COMPLETED, job.getState());
        assertEquals("done output", job.getOutput());
    }

    @Test
    @Order(8)
    void testFailJobSchedulesRetry() {
        jobService.enqueue(new Job("fail-svc", "false"));
        jobService.claimJob("worker-1");
        jobService.failJob("fail-svc", 1, "exit 1", "error");

        Job job = jobService.getJob("fail-svc");
        assertEquals(JobState.FAILED, job.getState());
        assertEquals(1, job.getAttempts());
        assertNotNull(job.getNextRetryAt());
    }

    @Test
    @Order(9)
    void testFailJobMovesToDlq() {
        Job job = new Job("dlq-svc", "invalid");
        job.setMaxRetries(1);
        jobService.enqueue(job);
        jobService.claimJob("worker-1");

        jobService.failJob("dlq-svc", 2, "exit 1", "error");

        Job result = jobService.getJob("dlq-svc");
        assertEquals(JobState.DEAD, result.getState());
    }

    @Test
    @Order(10)
    void testGetJobNotFound() {
        assertThrows(JobNotFoundException.class, () -> jobService.getJob("nonexistent"));
    }

    @Test
    @Order(11)
    void testListJobsByState() {
        jobService.enqueue(new Job("list-1", "echo 1"));
        jobService.enqueue(new Job("list-2", "echo 2"));

        assertEquals(2, jobService.listJobsByState(JobState.PENDING).size());
        assertEquals(0, jobService.listJobsByState(JobState.COMPLETED).size());
    }

    @Test
    @Order(12)
    void testCountByState() {
        jobService.enqueue(new Job("cnt-1", "echo 1"));
        jobService.enqueue(new Job("cnt-2", "echo 2"));

        assertEquals(2, jobService.countByState(JobState.PENDING));
    }
}
