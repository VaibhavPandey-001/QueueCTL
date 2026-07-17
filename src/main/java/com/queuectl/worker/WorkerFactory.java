package com.queuectl.worker;

import com.queuectl.repository.ConfigRepository;
import com.queuectl.service.JobService;
import com.queuectl.service.WorkerService;

/**
 * Factory for creating Worker instances.
 * Follows the Factory Pattern for worker creation.
 */
public class WorkerFactory {

    private final JobService jobService;
    private final WorkerService workerService;
    private final ConfigRepository configRepository;

    public WorkerFactory(JobService jobService, WorkerService workerService, ConfigRepository configRepository) {
        this.jobService = jobService;
        this.workerService = workerService;
        this.configRepository = configRepository;
    }

    /**
     * Creates a new Worker with an auto-generated ID.
     */
    public Worker create() {
        return new Worker(jobService, workerService, configRepository);
    }

    /**
     * Creates a new Worker with a specific ID.
     */
    public Worker create(String workerId) {
        return new Worker(workerId, jobService, workerService, configRepository);
    }
}
