package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.ConfigRepository;
import com.queuectl.repository.JobRepository;
import com.queuectl.utils.RetryCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for retry scheduling, delayed job processing, and stale lock cleanup.
 */
public class RetryService {

    private static final Logger logger = LoggerFactory.getLogger(RetryService.class);
    private final JobRepository jobRepository;
    private final ConfigRepository configRepository;

    public RetryService(DatabaseManager dbManager) {
        this.jobRepository = new JobRepository(dbManager);
        this.configRepository = new ConfigRepository(dbManager);
    }

    public RetryService(JobRepository jobRepository, ConfigRepository configRepository) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
    }

    /**
     * Reschedules a failed job for retry with exponential backoff.
     */
    public void scheduleRetry(Job job) {
        int baseBackoff = configRepository.getBaseBackoff();
        LocalDateTime nextRetry = RetryCalculator.calculateNextRetryTime(baseBackoff, job.getAttempts());
        jobRepository.updateFailed(job.getId(), job.getAttempts(), job.getLastError(), job.getOutput(), nextRetry);
        String delay = RetryCalculator.formatDelay(baseBackoff, job.getAttempts());
        logger.info("Retry scheduled for job {} in {} (attempt {}/{})",
                job.getId(), delay, job.getAttempts(), job.getMaxRetries());
    }

    /**
     * Processes delayed jobs whose run_at time has passed.
     */
    public int processDelayedJobs() {
        List<Job> allJobs = jobRepository.findAll();
        int activated = 0;
        for (Job job : allJobs) {
            if (job.getState() == JobState.PENDING && job.getRunAt() != null
                    && !job.getRunAt().isAfter(LocalDateTime.now())) {
                activated++;
            }
        }
        if (activated > 0) {
            logger.info("Activated {} delayed jobs", activated);
        }
        return activated;
    }

    /**
     * Moves FAILED jobs whose retry time has passed back to PENDING.
     * This is the core retry mechanism - workers only pick up PENDING jobs,
     * so the scheduler must transition eligible FAILED jobs.
     *
     * @return number of jobs reactivated
     */
    public int processRetries() {
        return jobRepository.reactivateRetriableJobs();
    }

    /**
     * Cleans up stale locks for jobs locked by workers that are no longer running.
     */
    public void cleanupStaleLocks() {
        jobRepository.cleanupStaleLocks();
    }

    /**
     * Checks if a job is eligible for execution.
     */
    public boolean isEligible(Job job) {
        if (job.getState() != JobState.PENDING) return false;
        if (job.isLocked()) return false;
        if (job.getRunAt() != null && job.getRunAt().isAfter(LocalDateTime.now())) return false;
        if (job.getNextRetryAt() != null && job.getNextRetryAt().isAfter(LocalDateTime.now())) return false;
        return true;
    }
}
