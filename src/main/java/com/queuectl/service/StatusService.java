package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.JobState;
import com.queuectl.repository.ConfigRepository;
import com.queuectl.repository.JobRepository;
import com.queuectl.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for system status and metrics.
 */
public class StatusService {

    private static final Logger logger = LoggerFactory.getLogger(StatusService.class);
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final ConfigRepository configRepository;

    public StatusService(DatabaseManager dbManager) {
        this.jobRepository = new JobRepository(dbManager);
        this.workerRepository = new WorkerRepository(dbManager);
        this.configRepository = new ConfigRepository(dbManager);
    }

    public StatusService(JobRepository jobRepository, WorkerRepository workerRepository, ConfigRepository configRepository) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
        this.configRepository = configRepository;
    }

    /**
     * Returns a comprehensive status map of the system.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("pending", jobRepository.countByState(JobState.PENDING));
        status.put("processing", jobRepository.countByState(JobState.PROCESSING));
        status.put("completed", jobRepository.countByState(JobState.COMPLETED));
        status.put("failed", jobRepository.countByState(JobState.FAILED));
        status.put("dead", jobRepository.countByState(JobState.DEAD));
        status.put("activeWorkers", workerRepository.countRunning());
        status.put("baseBackoff", configRepository.getBaseBackoff());
        status.put("defaultMaxRetry", configRepository.getDefaultMaxRetry());
        status.put("pollInterval", configRepository.getPollIntervalMs());
        status.put("jobTimeout", configRepository.getJobTimeoutSeconds());
        status.put("avgExecutionTime", String.format("%.1f", jobRepository.getAverageExecutionTime()) + "s");
        return status;
    }

    /**
     * Returns execution statistics.
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        int completed = jobRepository.countByState(JobState.COMPLETED);
        int failed = jobRepository.countByState(JobState.FAILED);
        int dead = jobRepository.countByState(JobState.DEAD);
        int totalFinished = completed + failed + dead;

        metrics.put("totalCompleted", completed);
        metrics.put("totalFailed", failed);
        metrics.put("totalDead", dead);
        metrics.put("successRate", totalFinished > 0
                ? String.format("%.1f%%", (completed * 100.0 / totalFinished))
                : "N/A");
        metrics.put("failureRate", totalFinished > 0
                ? String.format("%.1f%%", ((failed + dead) * 100.0 / totalFinished))
                : "N/A");
        metrics.put("avgExecutionTime", String.format("%.1f", jobRepository.getAverageExecutionTime()) + "s");
        return metrics;
    }
}
