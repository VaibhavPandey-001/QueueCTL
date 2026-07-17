package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.*;
import com.queuectl.exception.*;
import com.queuectl.repository.*;
import com.queuectl.utils.CommandValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core service for job lifecycle management: enqueue, claim, complete, fail.
 */
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    private final JobRepository jobRepository;
    private final ConfigRepository configRepository;
    private final LogRepository logRepository;

    public JobService(DatabaseManager dbManager) {
        this.jobRepository = new JobRepository(dbManager);
        this.configRepository = new ConfigRepository(dbManager);
        this.logRepository = new LogRepository(dbManager);
    }

    public JobService(JobRepository jobRepository, ConfigRepository configRepository, LogRepository logRepository) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
        this.logRepository = logRepository;
    }

    /**
     * Enqueues a new job. Validates input, checks for duplicates, inserts into DB.
     */
    public Job enqueue(Job job) {
        if (job.getId() == null || job.getId().isBlank()) {
            throw new IllegalArgumentException("Job ID cannot be null or blank");
        }
        if (job.getCommand() == null || job.getCommand().isBlank()) {
            throw new IllegalArgumentException("Job command cannot be null or blank");
        }
        CommandValidator.validateOrThrow(job.getCommand());

        if (jobRepository.existsById(job.getId())) {
            throw new DuplicateJobException(job.getId());
        }

        job.setState(JobState.PENDING);
        job.setAttempts(0);
        if (job.getMaxRetries() <= 0) {
            job.setMaxRetries(configRepository.getDefaultMaxRetry());
        }
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        job.setLocked(false);

        jobRepository.insert(job);

        logRepository.insert(new JobLog(job.getId(), null, "INFO", "Job enqueued: " + job.getCommand()));
        logger.info("Job enqueued: {} with command '{}'", job.getId(), job.getCommand());
        return job;
    }

    /**
     * Claims a pending job for a specific worker using transactional locking.
     */
    public Job claimJob(String workerId) {
        return jobRepository.claimJob(workerId).orElse(null);
    }

    /**
     * Marks a job as completed with its output.
     */
    public void completeJob(String jobId, String output) {
        jobRepository.updateCompleted(jobId, output);
        logRepository.insert(new JobLog(jobId, null, "INFO", "Job completed successfully"));
        logger.info("Job completed: {}", jobId);
    }

    /**
     * Handles job failure: increments attempts, schedules retry or moves to DLQ.
     */
    public void failJob(String jobId, int attempts, String error, String output) {
        int maxRetries = configRepository.getDefaultMaxRetry();
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        maxRetries = job.getMaxRetries();

        if (attempts > maxRetries) {
            jobRepository.moveToDlq(jobId, attempts, error, output);
            logRepository.insert(new JobLog(jobId, job.getWorkerId(), "ERROR",
                    "Job moved to DLQ after " + attempts + " attempts. Error: " + error));
            logger.warn("Job {} moved to DLQ after {} attempts", jobId, attempts);
        } else {
            int baseBackoff = configRepository.getBaseBackoff();
            java.time.LocalDateTime nextRetry = com.queuectl.utils.RetryCalculator.calculateNextRetryTime(baseBackoff, attempts);
            jobRepository.updateFailed(jobId, attempts, error, output, nextRetry);
            String delay = com.queuectl.utils.RetryCalculator.formatDelay(baseBackoff, attempts);
            logRepository.insert(new JobLog(jobId, job.getWorkerId(), "WARN",
                    "Job failed (attempt " + attempts + "/" + maxRetries + "). Retry in " + delay + ". Error: " + error));
            logger.info("Job {} failed, retry #{} scheduled in {}", jobId, attempts, delay);
        }
    }

    /**
     * Returns all jobs.
     */
    public List<Job> listJobs() {
        return jobRepository.findAll();
    }

    /**
     * Returns jobs filtered by state.
     */
    public List<Job> listJobsByState(JobState state) {
        return jobRepository.findByState(state);
    }

    /**
     * Finds a job by ID.
     */
    public Job getJob(String jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    /**
     * Gets the count of jobs in a given state.
     */
    public int countByState(JobState state) {
        return jobRepository.countByState(state);
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }
}
