package com.queuectl.entity;

import java.time.LocalDateTime;

public class JobLog {
    private long logId;
    private String jobId;
    private String workerId;
    private String level;
    private String message;
    private LocalDateTime timestamp;

    public JobLog() {}

    public JobLog(String jobId, String workerId, String level, String message) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.level = level;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public long getLogId() { return logId; }
    public void setLogId(long logId) { this.logId = logId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
