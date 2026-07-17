package com.queuectl.entity;

import java.time.LocalDateTime;

public class Job {
    private String id;
    private String command;
    private JobState state;
    private int attempts;
    private int maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private String workerId;
    private boolean locked;
    private int priority;
    private LocalDateTime runAt;
    private String output;

    public Job() {}

    public Job(String id, String command) {
        this.id = id;
        this.command = command;
        this.state = JobState.PENDING;
        this.attempts = 0;
        this.maxRetries = 3;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.locked = false;
        this.priority = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    @Override
    public String toString() {
        return "Job{id='" + id + "', command='" + command + "', state=" + state +
               ", attempts=" + attempts + ", maxRetries=" + maxRetries + "}";
    }
}
