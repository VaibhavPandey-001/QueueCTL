package com.queuectl.scheduler;

import com.queuectl.db.DatabaseManager;
import com.queuectl.repository.ConfigRepository;
import com.queuectl.service.RetryService;
import com.queuectl.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler responsible for periodic maintenance tasks:
 * - Cleaning up stale locks
 * - Cleaning up stale workers
 * - Processing delayed jobs
 * Runs on a fixed schedule in the background.
 */
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private static final long STALE_WORKER_THRESHOLD_MINUTES = 5;
    private static final long SCHEDULE_INTERVAL_SECONDS = 10;

    private final RetryService retryService;
    private final WorkerService workerService;
    private final ConfigRepository configRepository;
    private ScheduledExecutorService schedulerExecutor;

    public Scheduler(DatabaseManager dbManager) {
        this.retryService = new RetryService(dbManager);
        this.workerService = new WorkerService(dbManager);
        this.configRepository = new ConfigRepository(dbManager);
    }

    public Scheduler(RetryService retryService, WorkerService workerService, ConfigRepository configRepository) {
        this.retryService = retryService;
        this.workerService = workerService;
        this.configRepository = configRepository;
    }

    /**
     * Starts the scheduler with periodic maintenance tasks.
     */
    public void start() {
        schedulerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "queuectl-scheduler");
            t.setDaemon(true);
            return t;
        });

        schedulerExecutor.scheduleAtFixedRate(this::runMaintenanceCycle,
                SCHEDULE_INTERVAL_SECONDS, SCHEDULE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        logger.info("Scheduler started with {}s interval", SCHEDULE_INTERVAL_SECONDS);
    }

    /**
     * Stops the scheduler.
     */
    public void stop() {
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdown();
            try {
                if (!schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    schedulerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                schedulerExecutor.shutdownNow();
            }
        }
        logger.info("Scheduler stopped");
    }

    /**
     * Runs a single maintenance cycle.
     */
    private void runMaintenanceCycle() {
        try {
            retryService.processRetries();
            retryService.cleanupStaleLocks();
            workerService.cleanupStaleWorkers(STALE_WORKER_THRESHOLD_MINUTES);
            retryService.processDelayedJobs();
        } catch (Exception e) {
            logger.error("Error in scheduler maintenance cycle", e);
        }
    }
}
