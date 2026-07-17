package com.queuectl.exception;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String jobId) {
        super("Job not found with ID: " + jobId);
    }
}
