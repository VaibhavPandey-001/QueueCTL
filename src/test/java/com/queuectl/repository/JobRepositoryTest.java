package com.queuectl.repository;

import com.queuectl.TestBase;
import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JobRepository: insert, query, claim, update, delete.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobRepositoryTest extends TestBase {

    private DatabaseManager dbManager;
    private JobRepository repository;

    @BeforeEach
    void setUp() {
        dbManager = createTestDatabase();
        repository = new JobRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsertAndFindById() {
        Job job = createJob("test-1", "echo hello");
        repository.insert(job);

        Optional<Job> found = repository.findById("test-1");
        assertTrue(found.isPresent());
        assertEquals("test-1", found.get().getId());
        assertEquals("echo hello", found.get().getCommand());
        assertEquals(JobState.PENDING, found.get().getState());
    }

    @Test
    @Order(2)
    void testExistsById() {
        assertFalse(repository.existsById("nonexistent"));

        repository.insert(createJob("exists-test", "ls"));
        assertTrue(repository.existsById("exists-test"));
    }

    @Test
    @Order(3)
    void testFindAll() {
        repository.insert(createJob("all-1", "echo 1"));
        repository.insert(createJob("all-2", "echo 2"));

        List<Job> jobs = repository.findAll();
        assertEquals(2, jobs.size());
    }

    @Test
    @Order(4)
    void testFindByState() {
        repository.insert(createJob("state-1", "echo 1"));
        repository.insert(createJob("state-2", "echo 2"));

        List<Job> pending = repository.findByState(JobState.PENDING);
        assertEquals(2, pending.size());

        repository.updateState("state-1", JobState.COMPLETED);
        List<Job> completed = repository.findByState(JobState.COMPLETED);
        assertEquals(1, completed.size());
        assertEquals("state-1", completed.get(0).getId());
    }

    @Test
    @Order(5)
    void testClaimJob() {
        repository.insert(createJob("claim-1", "echo claim"));

        Optional<Job> claimed = repository.claimJob("worker-test");
        assertTrue(claimed.isPresent());
        assertEquals("claim-1", claimed.get().getId());
        assertEquals("worker-test", claimed.get().getWorkerId());
        assertTrue(claimed.get().isLocked());
        assertEquals(JobState.PROCESSING, claimed.get().getState());
    }

    @Test
    @Order(6)
    void testClaimJobPreventsDuplicate() {
        repository.insert(createJob("dup-1", "echo dup"));

        Optional<Job> first = repository.claimJob("worker-1");
        assertTrue(first.isPresent());

        Optional<Job> second = repository.claimJob("worker-2");
        assertFalse(second.isPresent());
    }

    @Test
    @Order(7)
    void testUpdateCompleted() {
        repository.insert(createJob("complete-1", "echo done"));
        repository.claimJob("worker-c");

        repository.updateCompleted("complete-1", "done output");

        Job job = repository.findById("complete-1").get();
        assertEquals(JobState.COMPLETED, job.getState());
        assertEquals("done output", job.getOutput());
        assertFalse(job.isLocked());
    }

    @Test
    @Order(8)
    void testUpdateFailed() {
        repository.insert(createJob("fail-1", "false"));
        repository.claimJob("worker-f");

        repository.updateFailed("fail-1", 1, "exit code 1", "error output", LocalDateTime.now().plusSeconds(5));

        Job job = repository.findById("fail-1").get();
        assertEquals(JobState.FAILED, job.getState());
        assertEquals(1, job.getAttempts());
        assertEquals("exit code 1", job.getLastError());
        assertNotNull(job.getNextRetryAt());
    }

    @Test
    @Order(9)
    void testMoveToDlq() {
        repository.insert(createJob("dlq-1", "invalid"));
        repository.claimJob("worker-d");

        repository.moveToDlq("dlq-1", 5, "too many failures", "output");

        Job job = repository.findById("dlq-1").get();
        assertEquals(JobState.DEAD, job.getState());
        assertEquals(5, job.getAttempts());
    }

    @Test
    @Order(10)
    void testRetryFromDlq() {
        repository.insert(createJob("dlq-retry", "echo retry"));
        repository.moveToDlq("dlq-retry", 3, "error", "");

        repository.retryFromDlq("dlq-retry");

        Job job = repository.findById("dlq-retry").get();
        assertEquals(JobState.PENDING, job.getState());
        assertEquals(0, job.getAttempts());
        assertNull(job.getLastError());
    }

    @Test
    @Order(11)
    void testUnlockJob() {
        repository.insert(createJob("unlock-1", "echo unlock"));
        repository.claimJob("worker-u");

        repository.unlockJob("unlock-1");

        Job job = repository.findById("unlock-1").get();
        assertFalse(job.isLocked());
        assertNull(job.getWorkerId());
    }

    @Test
    @Order(12)
    void testDelete() {
        repository.insert(createJob("delete-1", "echo delete"));
        assertTrue(repository.existsById("delete-1"));

        repository.delete("delete-1");
        assertFalse(repository.existsById("delete-1"));
    }

    @Test
    @Order(13)
    void testCountByState() {
        repository.insert(createJob("count-1", "echo 1"));
        repository.insert(createJob("count-2", "echo 2"));
        repository.insert(createJob("count-3", "echo 3"));

        assertEquals(3, repository.countByState(JobState.PENDING));
        assertEquals(0, repository.countByState(JobState.COMPLETED));
    }

    private Job createJob(String id, String command) {
        Job job = new Job(id, command);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return job;
    }
}
