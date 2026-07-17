package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.WorkerInfo;
import com.queuectl.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for worker registration, heartbeat, and lifecycle management.
 */
public class WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);
    private final WorkerRepository workerRepository;

    public WorkerService(DatabaseManager dbManager) {
        this.workerRepository = new WorkerRepository(dbManager);
    }

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    /**
     * Registers a new worker in the database.
     */
    public void register(WorkerInfo worker) {
        workerRepository.register(worker);
        logger.info("Worker registered: {}", worker.getWorkerId());
    }

    /**
     * Updates the heartbeat timestamp for a worker.
     */
    public void heartbeat(String workerId) {
        workerRepository.updateHeartbeat(workerId);
    }

    /**
     * Marks a worker as stopped.
     */
    public void stop(String workerId) {
        workerRepository.updateStatus(workerId, "STOPPED");
        logger.info("Worker stopped: {}", workerId);
    }

    /**
     * Unregisters a worker completely.
     */
    public void unregister(String workerId) {
        workerRepository.unregister(workerId);
    }

    /**
     * Returns all running workers.
     */
    public List<WorkerInfo> getRunningWorkers() {
        return workerRepository.findAllRunning();
    }

    /**
     * Returns all workers.
     */
    public List<WorkerInfo> getAllWorkers() {
        return workerRepository.findAll();
    }

    /**
     * Returns count of running workers.
     */
    public int countRunning() {
        return workerRepository.countRunning();
    }

    /**
     * Cleans up stale workers that haven't sent heartbeats.
     */
    public void cleanupStaleWorkers(long thresholdMinutes) {
        workerRepository.cleanupStaleWorkers(thresholdMinutes);
    }

    public WorkerRepository getWorkerRepository() {
        return workerRepository;
    }
}
