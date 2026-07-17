package com.queuectl.utils;

import java.util.Arrays;
import java.util.List;

public final class CommandValidator {

    private static final List<String> BLOCKED_COMMANDS = List.of(
        "rm -rf /", "rm -rf /*", "mkfs", ":(){:|:&};:", "dd if=/dev/zero"
    );

    private CommandValidator() {}

    public static boolean isValid(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        for (String blocked : BLOCKED_COMMANDS) {
            if (command.contains(blocked)) {
                return false;
            }
        }
        return true;
    }

    public static String validateOrThrow(String command) {
        if (!isValid(command)) {
            throw new IllegalArgumentException("Invalid or dangerous command: " + command);
        }
        return command;
    }
}
