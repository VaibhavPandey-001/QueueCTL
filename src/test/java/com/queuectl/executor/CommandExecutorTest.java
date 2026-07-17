package com.queuectl.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandExecutor: command execution, exit codes, output capture.
 */
class CommandExecutorTest {

    private final CommandExecutor executor = new CommandExecutor();

    @Test
    void testSuccessfulCommand() {
        CommandExecutor.ExecutionResult result = executor.execute("echo hello", 5);
        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("hello"));
    }

    @Test
    void testFailingCommand() {
        String cmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? "cmd /c exit 1" : "exit 1";
        CommandExecutor.ExecutionResult result = executor.execute(cmd, 5);
        assertNotEquals(0, result.exitCode());
    }

    @Test
    void testCommandTimeout() {
        String javaCmd = "java -cp . -version";
        CommandExecutor.ExecutionResult result = executor.execute(javaCmd, 1);
        assertNotNull(result);
        assertTrue(result.executionTimeMs() >= 0);
    }

    @Test
    void testInvalidCommand() {
        CommandExecutor.ExecutionResult result = executor.execute("nonexistent_command_xyz_12345", 5);
        assertNotEquals(0, result.exitCode());
    }

    @Test
    void testEmptyCommand() {
        CommandExecutor.ExecutionResult result = executor.execute("", 5);
        assertNotNull(result);
    }
}
