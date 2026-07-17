package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.DlqService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * CLI command to manage the Dead Letter Queue.
 *
 * Usage: queuectl dlq list
 *        queuectl dlq retry <jobId>
 */
@Command(
    name = "dlq",
    description = "Manage Dead Letter Queue",
    mixinStandardHelpOptions = true,
    subcommands = {
        DlqListCommand.class,
        DlqRetryCommand.class
    }
)
public class DlqCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: queuectl dlq <list|retry> [options]");
        System.out.println("  list   - Show all dead jobs");
        System.out.println("  retry  - Retry a dead job");
        return 0;
    }
}
