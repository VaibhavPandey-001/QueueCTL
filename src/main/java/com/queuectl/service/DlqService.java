package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import com.queuectl.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for Dead Letter Queue operations.
 */
public class DlqService {

    private static final Logger logger = LoggerFactory.getLogger(DlqService.class);
    private final JobRepository jobRepository;
    private final LogRepository logRepository;

    public DlqService(DatabaseManager dbManager) {
        this.jobRepository = new JobRepository(dbManager);
        this.logRepository = new LogRepository(dbManager);
    }

    public DlqService(JobRepository jobRepository, LogRepository logRepository) {
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
    }

    /**
     * Lists all jobs in the Dead Letter Queue.
     */
    public List<Job> listDeadJobs() {
        return jobRepository.findByState(JobState.DEAD);
    }

    /**
     * Retries a dead job by moving it back to PENDING state.
     * Resets attempts to 0 and clears the last error.
     */
    public Job retryJob(String jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new com.queuectl.exception.JobNotFoundException(jobId));

        if (job.getState() != JobState.DEAD) {
            throw new IllegalStateException("Job " + jobId + " is not in DEAD state. Current state: " + job.getState());
        }

        jobRepository.retryFromDlq(jobId);
        logRepository.insert(new com.queuectl.entity.JobLog(jobId, null, "INFO",
                "Job retried from DLQ. Attempts reset to 0."));
        logger.info("Job {} retried from DLQ", jobId);

        return jobRepository.findById(jobId).orElseThrow(
                () -> new com.queuectl.exception.JobNotFoundException(jobId));
    }

    /**
     * Returns the count of dead jobs.
     */
    public int countDeadJobs() {
        return jobRepository.countByState(JobState.DEAD);
    }

    /**
     * Permanently deletes a dead job.
     */
    public void purgeJob(String jobId) {
        jobRepository.delete(jobId);
        logger.info("Dead job {} purged from DLQ", jobId);
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }
}
