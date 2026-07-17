package com.queuectl.utils;

import java.time.LocalDateTime;

public final class RetryCalculator {

    private RetryCalculator() {}

    /**
     * Calculates delay in seconds using exponential backoff.
     * delay = base^attempt
     */
    public static long calculateDelaySeconds(int base, int attempt) {
        if (base <= 0) base = 2;
        if (attempt <= 0) attempt = 1;
        return (long) Math.pow(base, attempt);
    }

    /**
     * Calculates the next retry time.
     */
    public static LocalDateTime calculateNextRetryTime(int baseBackoff, int attempt) {
        long delaySeconds = calculateDelaySeconds(baseBackoff, attempt);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * Returns a human-readable string for the backoff delay.
     */
    public static String formatDelay(int base, int attempt) {
        long seconds = calculateDelaySeconds(base, attempt);
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }
}
