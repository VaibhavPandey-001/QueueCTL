package com.queuectl.entity;

import java.time.LocalDateTime;

public class WorkerInfo {
    private String workerId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime lastHeartbeat;
    private long pid;

    public WorkerInfo() {}

    public WorkerInfo(String workerId, long pid) {
        this.workerId = workerId;
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
        this.pid = pid;
    }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    @Override
    public String toString() {
        return "WorkerInfo{workerId='" + workerId + "', status='" + status +
               "', pid=" + pid + "}";
    }
}
