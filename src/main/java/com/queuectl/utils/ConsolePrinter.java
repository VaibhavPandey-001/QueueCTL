package com.queuectl.utils;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.entity.WorkerInfo;

import java.util.List;
import java.util.Map;

public final class ConsolePrinter {

    private ConsolePrinter() {}

    public static void printJobList(List<Job> jobs) {
        if (jobs.isEmpty()) {
            System.out.println("No jobs found.");
            return;
        }
        String header = String.format("%-20s %-30s %-10s %-12s %-10s %-8s",
                "ID", "COMMAND", "ATTEMPTS", "STATE", "CREATED", "WORKER");
        System.out.println(header);
        System.out.println("-".repeat(90));
        for (Job job : jobs) {
            String created = job.getCreatedAt() != null ? job.getCreatedAt().toString().substring(0, 19) : "N/A";
            String worker = job.getWorkerId() != null ? job.getWorkerId() : "-";
            String cmd = job.getCommand();
            if (cmd != null && cmd.length() > 28) {
                cmd = cmd.substring(0, 25) + "...";
            }
            String line = String.format("%-20s %-30s %-10s %-12s %-10s %-8s",
                    job.getId(),
                    cmd,
                    job.getAttempts() + "/" + job.getMaxRetries(),
                    job.getState(),
                    created,
                    worker);
            System.out.println(line);
        }
        System.out.println("\nTotal: " + jobs.size() + " job(s)");
    }

    public static void printWorkerList(List<WorkerInfo> workers) {
        if (workers.isEmpty()) {
            System.out.println("No active workers.");
            return;
        }
        String header = String.format("%-20s %-12s %-8s %-25s %-25s",
                "WORKER ID", "STATUS", "PID", "STARTED", "LAST HEARTBEAT");
        System.out.println(header);
        System.out.println("-".repeat(90));
        for (WorkerInfo w : workers) {
            String started = w.getStartedAt() != null ? w.getStartedAt().toString().substring(0, 19) : "N/A";
            String hb = w.getLastHeartbeat() != null ? w.getLastHeartbeat().toString().substring(0, 19) : "N/A";
            System.out.println(String.format("%-20s %-12s %-8s %-25s %-25s",
                    w.getWorkerId(), w.getStatus(), w.getPid(), started, hb));
        }
    }

    public static void printStatus(Map<String, Object> status) {
        System.out.println("=== QueueCTL Status ===");
        System.out.println();
        System.out.println("Jobs:");
        System.out.println("  Pending    : " + status.getOrDefault("pending", 0));
        System.out.println("  Processing : " + status.getOrDefault("processing", 0));
        System.out.println("  Completed  : " + status.getOrDefault("completed", 0));
        System.out.println("  Failed     : " + status.getOrDefault("failed", 0));
        System.out.println("  Dead (DLQ) : " + status.getOrDefault("dead", 0));
        System.out.println();
        System.out.println("Workers:");
        System.out.println("  Active     : " + status.getOrDefault("activeWorkers", 0));
        System.out.println();
        System.out.println("Config:");
        System.out.println("  Base Backoff       : " + status.getOrDefault("baseBackoff", 2) + "s");
        System.out.println("  Default Max Retry  : " + status.getOrDefault("defaultMaxRetry", 3));
        System.out.println("  Poll Interval      : " + status.getOrDefault("pollInterval", 1000) + "ms");
        System.out.println("  Job Timeout        : " + status.getOrDefault("jobTimeout", 300) + "s");
    }

    public static void printConfig(Map<String, String> config) {
        System.out.println("=== QueueCTL Config ===");
        System.out.println();
        System.out.println("  base-backoff      : " + config.getOrDefault("base-backoff", "2"));
        System.out.println("  default-max-retry : " + config.getOrDefault("default-max-retry", "3"));
        System.out.println("  poll-interval     : " + config.getOrDefault("poll-interval", "1000"));
        System.out.println("  job-timeout       : " + config.getOrDefault("job-timeout", "300"));
    }

    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    public static void printError(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }
}
