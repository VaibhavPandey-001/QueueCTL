package com.queuectl.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ClockUtil {

    private ClockUtil() {}

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDateTime fromEpochMillis(long millis) {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()
        );
    }
}
