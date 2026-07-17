package com.queuectl.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Root CLI command for QueueCTL.
 * Registers all subcommands.
 */
@Command(
    name = "queuectl",
    description = "QueueCTL - CLI-based Background Job Queue System",
    mixinStandardHelpOptions = true,
    version = "QueueCTL 1.0.0",
    subcommands = {
        EnqueueCommand.class,
        WorkerCommand.class,
        ListCommand.class,
        StatusCommand.class,
        DlqCommand.class,
        ConfigCommand.class,
        HelpCommand.class
    }
)
public class QueueCTLCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("QueueCTL - CLI-based Background Job Queue System v1.0.0");
        System.out.println();
        System.out.println("Usage: queuectl <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  enqueue    Enqueue a new job");
        System.out.println("  worker     Manage workers (start/stop)");
        System.out.println("  list       List all jobs");
        System.out.println("  status     Show system status");
        System.out.println("  dlq        Manage Dead Letter Queue");
        System.out.println("  config     Manage configuration");
        System.out.println("  help       Show help information");
        System.out.println();
        System.out.println("Use 'queuectl <command> --help' for more information on a command.");
        return 0;
    }
}
