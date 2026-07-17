package com.queuectl.service;

import com.queuectl.TestBase;
import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.exception.JobNotFoundException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DlqService: list, retry, purge dead jobs.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DlqServiceTest extends TestBase {

    private DatabaseManager dbManager;
    private DlqService dlqService;

    @BeforeEach
    void setUp() {
        dbManager = createTestDatabase();
        dlqService = new DlqService(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @Order(1)
    void testListDeadJobsEmpty() {
        assertTrue(dlqService.listDeadJobs().isEmpty());
    }

    @Test
    @Order(2)
    void testListDeadJobs() {
        // Create a job and move it to DLQ
        com.queuectl.repository.JobRepository jobRepo = new com.queuectl.repository.JobRepository(dbManager);
        Job job = new Job("dlq-test", "echo dlq");
        job.setCreatedAt(java.time.LocalDateTime.now());
        job.setUpdatedAt(java.time.LocalDateTime.now());
        jobRepo.insert(job);
        jobRepo.moveToDlq("dlq-test", 3, "max retries exceeded", "output");

        assertEquals(1, dlqService.listDeadJobs().size());
        assertEquals("dlq-test", dlqService.listDeadJobs().get(0).getId());
    }

    @Test
    @Order(3)
    void testRetryJob() {
        com.queuectl.repository.JobRepository jobRepo = new com.queuectl.repository.JobRepository(dbManager);
        Job job = new Job("dlq-retry-test", "echo retry");
        job.setCreatedAt(java.time.LocalDateTime.now());
        job.setUpdatedAt(java.time.LocalDateTime.now());
        jobRepo.insert(job);
        jobRepo.moveToDlq("dlq-retry-test", 5, "error", "output");

        Job retried = dlqService.retryJob("dlq-retry-test");
        assertEquals(JobState.PENDING, retried.getState());
        assertEquals(0, retried.getAttempts());
        assertNull(retried.getLastError());
    }

    @Test
    @Order(4)
    void testRetryNonDeadJobThrows() {
        com.queuectl.repository.JobRepository jobRepo = new com.queuectl.repository.JobRepository(dbManager);
        Job job = new Job("not-dead", "echo alive");
        job.setCreatedAt(java.time.LocalDateTime.now());
        job.setUpdatedAt(java.time.LocalDateTime.now());
        jobRepo.insert(job);

        assertThrows(IllegalStateException.class, () -> dlqService.retryJob("not-dead"));
    }

    @Test
    @Order(5)
    void testRetryNonexistentJobThrows() {
        assertThrows(JobNotFoundException.class, () -> dlqService.retryJob("no-such-job"));
    }

    @Test
    @Order(6)
    void testPurgeJob() {
        com.queuectl.repository.JobRepository jobRepo = new com.queuectl.repository.JobRepository(dbManager);
        Job job = new Job("purge-test", "echo purge");
        job.setCreatedAt(java.time.LocalDateTime.now());
        job.setUpdatedAt(java.time.LocalDateTime.now());
        jobRepo.insert(job);
        jobRepo.moveToDlq("purge-test", 3, "error", "");

        dlqService.purgeJob("purge-test");
        assertFalse(jobRepo.existsById("purge-test"));
    }

    @Test
    @Order(7)
    void testCountDeadJobs() {
        com.queuectl.repository.JobRepository jobRepo = new com.queuectl.repository.JobRepository(dbManager);
        Job job = new Job("count-dead", "echo count");
        job.setCreatedAt(java.time.LocalDateTime.now());
        job.setUpdatedAt(java.time.LocalDateTime.now());
        jobRepo.insert(job);
        jobRepo.moveToDlq("count-dead", 3, "error", "");

        assertEquals(1, dlqService.countDeadJobs());
    }
}
