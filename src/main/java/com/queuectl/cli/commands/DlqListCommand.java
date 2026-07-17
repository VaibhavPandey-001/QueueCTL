package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.DlqService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Lists all dead jobs in the Dead Letter Queue.
 *
 * Usage: queuectl dlq list
 */
@Command(
    name = "list",
    description = "Show all jobs in the Dead Letter Queue",
    mixinStandardHelpOptions = true
)
public class DlqListCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            DlqService dlqService = new DlqService(AppConfig.getDatabaseManager());
            ConsolePrinter.printJobList(dlqService.listDeadJobs());
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to list DLQ jobs: " + e.getMessage());
            return 1;
        }
    }
}
