package com.queuectl.worker;

import com.queuectl.db.DatabaseManager;
import com.queuectl.repository.ConfigRepository;
import com.queuectl.service.JobService;
import com.queuectl.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages multiple worker threads.
 * Handles starting, stopping, and coordinating workers.
 */
public class WorkerManager {

    private static final Logger logger = LoggerFactory.getLogger(WorkerManager.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final JobService jobService;
    private final WorkerService workerService;
    private final ConfigRepository configRepository;
    private final List<Worker> workers;
    private ExecutorService executorService;

    public WorkerManager(DatabaseManager dbManager) {
        this.jobService = new JobService(dbManager);
        this.workerService = new WorkerService(dbManager);
        this.configRepository = new ConfigRepository(dbManager);
        this.workers = new ArrayList<>();
    }

    public WorkerManager(JobService jobService, WorkerService workerService, ConfigRepository configRepository) {
        this.jobService = jobService;
        this.workerService = workerService;
        this.configRepository = configRepository;
        this.workers = new ArrayList<>();
    }

    /**
     * Starts a specified number of workers in parallel.
     *
     * @param count number of workers to start
     */
    public void startWorkers(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Worker count must be at least 1");
        }

        WorkerFactory factory = new WorkerFactory(jobService, workerService, configRepository);
        executorService = Executors.newFixedThreadPool(count);

        for (int i = 0; i < count; i++) {
            Worker worker = factory.create();
            workers.add(worker);
            executorService.submit(worker);
            logger.info("Submitted worker {} to thread pool", worker.getWorkerId());
        }

        logger.info("Started {} workers", count);
    }

    /**
     * Gracefully stops all workers.
     * Workers finish their current job before exiting.
     */
    public void stopAllWorkers() {
        logger.info("Initiating graceful shutdown of {} workers", workers.size());

        for (Worker worker : workers) {
            worker.shutdown();
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in {} seconds, forcing shutdown", SHUTDOWN_TIMEOUT_SECONDS);
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }

        workers.clear();
        logger.info("All workers stopped");
    }

    /**
     * Returns the number of active workers.
     */
    public int getActiveWorkerCount() {
        return (int) workers.stream().filter(Worker::isRunning).count();
    }

    /**
     * Returns all worker IDs.
     */
    public List<String> getWorkerIds() {
        return workers.stream().map(Worker::getWorkerId).toList();
    }
}
