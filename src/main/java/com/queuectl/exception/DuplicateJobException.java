package com.queuectl.exception;

public class DuplicateJobException extends RuntimeException {
    public DuplicateJobException(String jobId) {
        super("Job already exists with ID: " + jobId);
    }
}
