package com.queuectl;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import com.queuectl.service.JobService;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: end-to-end enqueue -> claim -> complete flow.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest extends TestBase {

    private DatabaseManager dbManager;
    private JobService jobService;
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        dbManager = createTestDatabase();
        jobService = new JobService(dbManager);
        jobRepository = new JobRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @Order(1)
    void testEndToEndEnqueueToComplete() {
        // 1. Enqueue a job
        Job job = new Job("e2e-1", "echo hello world");
        jobService.enqueue(job);

        // 2. Verify it's PENDING
        Job pending = jobService.getJob("e2e-1");
        assertEquals(JobState.PENDING, pending.getState());
        assertEquals(0, pending.getAttempts());

        // 3. Claim the job
        Job claimed = jobService.claimJob("test-worker");
        assertNotNull(claimed);
        assertEquals("e2e-1", claimed.getId());

        // 4. Complete the job
        jobService.completeJob("e2e-1", "hello world");

        // 5. Verify it's COMPLETED
        Job completed = jobService.getJob("e2e-1");
        assertEquals(JobState.COMPLETED, completed.getState());
        assertEquals("hello world", completed.getOutput());
    }

    @Test
    @Order(2)
    void testEndToEndEnqueueToFailToRetryToDlq() {
        // 1. Enqueue with maxRetries = 1
        Job job = new Job("e2e-dlq", "false");
        job.setMaxRetries(1);
        jobService.enqueue(job);

        // 2. Claim and fail (attempt 2 exceeds maxRetries=1)
        jobService.claimJob("test-worker");
        jobService.failJob("e2e-dlq", 2, "exit code 1", "error output");

        // 3. Verify it's DEAD (DLQ)
        Job dead = jobService.getJob("e2e-dlq");
        assertEquals(JobState.DEAD, dead.getState());

        // 4. Retry from DLQ
        com.queuectl.service.DlqService dlqService = new com.queuectl.service.DlqService(dbManager);
        Job retried = dlqService.retryJob("e2e-dlq");
        assertEquals(JobState.PENDING, retried.getState());
        assertEquals(0, retried.getAttempts());
    }

    @Test
    @Order(3)
    void testEndToEndEnqueueToFailToRetry() {
        // 1. Enqueue with maxRetries = 2
        Job job = new Job("e2e-retry", "false");
        job.setMaxRetries(2);
        jobService.enqueue(job);

        // 2. Claim and fail (attempt 1)
        jobService.claimJob("test-worker");
        jobService.failJob("e2e-retry", 1, "exit 1", "error");
        Job afterFail1 = jobService.getJob("e2e-retry");
        assertEquals(JobState.FAILED, afterFail1.getState());
        assertEquals(1, afterFail1.getAttempts());
        assertNotNull(afterFail1.getNextRetryAt());

        // 3. Claim again (retry) and fail (attempt 2)
        jobService.claimJob("test-worker");
        jobService.failJob("e2e-retry", 2, "exit 1", "error");
        Job afterFail2 = jobService.getJob("e2e-retry");
        assertEquals(JobState.FAILED, afterFail2.getState());
        assertEquals(2, afterFail2.getAttempts());

        // 4. Claim again (retry) and fail (attempt 3, exceeds maxRetries=2)
        jobService.claimJob("test-worker");
        jobService.failJob("e2e-retry", 3, "exit 1", "error");
        Job afterFail3 = jobService.getJob("e2e-retry");
        assertEquals(JobState.DEAD, afterFail3.getState());
    }

    @Test
    @Order(4)
    void testDatabasePersistence() {
        // Create a job and close DB
        Job job = new Job("persist-1", "echo persist");
        jobService.enqueue(job);

        // Reopen DB
        dbManager.close();
        DatabaseManager newDb = reopenTestDatabase();
        JobService newService = new JobService(newDb);

        // Verify job still exists
        Job found = newService.getJob("persist-1");
        assertEquals("persist-1", found.getId());
        assertEquals("echo persist", found.getCommand());
        assertEquals(JobState.PENDING, found.getState());

        newDb.close();
    }

    @Test
    @Order(5)
    void testMultipleJobsOrdering() {
        // Enqueue jobs with different priorities
        Job low = new Job("low-pri", "echo low");
        low.setPriority(1);
        jobService.enqueue(low);

        Job high = new Job("high-pri", "echo high");
        high.setPriority(10);
        jobService.enqueue(high);

        Job mid = new Job("mid-pri", "echo mid");
        mid.setPriority(5);
        jobService.enqueue(mid);

        // First claimed should be highest priority
        Job claimed = jobService.claimJob("test-worker");
        assertEquals("high-pri", claimed.getId());
    }

    @Test
    @Order(6)
    void testConcurrentClaimPrevention() {
        Job job = new Job("concurrent-1", "echo concurrent");
        jobService.enqueue(job);

        // Two workers try to claim
        Job claimed1 = jobService.claimJob("worker-a");
        Job claimed2 = jobService.claimJob("worker-b");

        assertNotNull(claimed1);
        assertNull(claimed2); // Only one should succeed
    }
}
