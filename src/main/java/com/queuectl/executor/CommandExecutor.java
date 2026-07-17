package com.queuectl.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.*;

/**
 * Executes shell commands using ProcessBuilder.
 * Captures stdout, stderr, exit code, and execution time.
 */
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    /**
     * Result of a command execution.
     */
    public record ExecutionResult(int exitCode, String output, long executionTimeMs) {}

    /**
     * Executes a shell command and captures the output.
     *
     * @param command the command to execute
     * @param timeoutSeconds maximum execution time in seconds
     * @return execution result with exit code, output, and timing
     */
    public ExecutionResult execute(String command, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        logger.info("Executing command: {}", command);

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            ExecutorService outputReader = Executors.newSingleThreadExecutor();
            Future<String> outputFuture = outputReader.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append(System.lineSeparator());
                    }
                }
                return sb.toString().trim();
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                long elapsed = System.currentTimeMillis() - startTime;
                String errMsg = "Command timed out after " + timeoutSeconds + "s: " + command;
                logger.warn(errMsg);
                try {
                    String partialOutput = outputFuture.get(2, TimeUnit.SECONDS);
                    outputReader.shutdownNow();
                    return new ExecutionResult(-1, partialOutput + "\n" + errMsg, elapsed);
                } catch (Exception ex) {
                    outputReader.shutdownNow();
                    return new ExecutionResult(-1, errMsg, elapsed);
                }
            }

            String resultOutput;
            try {
                resultOutput = outputFuture.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                resultOutput = "";
            }
            outputReader.shutdownNow();

            int exitCode = process.exitValue();
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("Command completed: exitCode={}, elapsed={}ms", exitCode, elapsed);
            return new ExecutionResult(exitCode, resultOutput, elapsed);

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("Failed to execute command: {}", command, e);
            return new ExecutionResult(-1, "IOException: " + e.getMessage(), elapsed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - startTime;
            return new ExecutionResult(-1, "Interrupted: " + e.getMessage(), elapsed);
        }
    }
}
