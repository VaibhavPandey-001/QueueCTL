package com.queuectl.worker;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.entity.WorkerInfo;
import com.queuectl.executor.CommandExecutor;
import com.queuectl.executor.CommandExecutor.ExecutionResult;
import com.queuectl.repository.ConfigRepository;
import com.queuectl.service.JobService;
import com.queuectl.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker that polls the database for pending jobs and executes them.
 * Runs in a loop until stopped. Sends periodic heartbeats.
 */
public class Worker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private static final long HEARTBEAT_INTERVAL_MS = 5000;

    private final String workerId;
    private final JobService jobService;
    private final WorkerService workerService;
    private final ConfigRepository configRepository;
    private final CommandExecutor executor;
    private final AtomicBoolean running;
    private final WorkerInfo workerInfo;

    private Thread workerThread;

    public Worker(JobService jobService, WorkerService workerService, ConfigRepository configRepository) {
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        this.jobService = jobService;
        this.workerService = workerService;
        this.configRepository = configRepository;
        this.executor = new CommandExecutor();
        this.running = new AtomicBoolean(true);
        this.workerInfo = new WorkerInfo(workerId, ProcessHandle.current().pid());
    }

    public Worker(String workerId, JobService jobService, WorkerService workerService, ConfigRepository configRepository) {
        this.workerId = workerId;
        this.jobService = jobService;
        this.workerService = workerService;
        this.configRepository = configRepository;
        this.executor = new CommandExecutor();
        this.running = new AtomicBoolean(true);
        this.workerInfo = new WorkerInfo(workerId, ProcessHandle.current().pid());
    }

    @Override
    public void run() {
        workerThread = Thread.currentThread();
        workerService.register(workerInfo);
        logger.info("Worker {} started (PID: {})", workerId, workerInfo.getPid());

        long lastHeartbeat = System.currentTimeMillis();

        while (running.get()) {
            try {
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
                    workerService.heartbeat(workerId);
                    lastHeartbeat = now;
                }

                Job job = jobService.claimJob(workerId);
                if (job != null) {
                    processJob(job);
                }

                long pollInterval = configRepository.getPollIntervalMs();
                Thread.sleep(Math.max(pollInterval, 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Worker {} interrupted", workerId);
                break;
            } catch (Exception e) {
                logger.error("Worker {} encountered an error", workerId, e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        workerService.stop(workerId);
        logger.info("Worker {} stopped", workerId);
    }

    /**
     * Processes a single job: execute, handle success/failure, retry or DLQ.
     */
    private void processJob(Job job) {
        logger.info("Worker {} processing job: {}", workerId, job.getId());
        long timeoutSeconds = configRepository.getJobTimeoutSeconds();

        ExecutionResult result = executor.execute(job.getCommand(), timeoutSeconds);
        String output = result.output() != null ? result.output() : "";
        int newAttempts = job.getAttempts() + 1;

        if (result.exitCode() == 0) {
            jobService.completeJob(job.getId(), output);
        } else {
            String error = "Exit code: " + result.exitCode() + ". Output: " + output;
            jobService.failJob(job.getId(), newAttempts, error, output);
        }
    }

    /**
     * Requests a graceful shutdown. Worker will finish the current job before stopping.
     */
    public void shutdown() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public String getWorkerId() {
        return workerId;
    }

    public boolean isRunning() {
        return running.get();
    }

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }
}
