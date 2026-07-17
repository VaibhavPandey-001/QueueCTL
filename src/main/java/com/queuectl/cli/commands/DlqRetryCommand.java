package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.DlqService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Retries a dead job by moving it back to PENDING state.
 *
 * Usage: queuectl dlq retry <jobId>
 */
@Command(
    name = "retry",
    description = "Retry a job from the Dead Letter Queue",
    mixinStandardHelpOptions = true
)
public class DlqRetryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "ID of the dead job to retry")
    private String jobId;

    @Override
    public Integer call() {
        try {
            DlqService dlqService = new DlqService(AppConfig.getDatabaseManager());
            dlqService.retryJob(jobId);
            ConsolePrinter.printSuccess("Job '" + jobId + "' moved back to PENDING queue.");
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to retry job: " + e.getMessage());
            return 1;
        }
    }
}
