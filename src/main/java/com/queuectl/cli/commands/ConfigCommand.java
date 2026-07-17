package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.ConfigService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * CLI command to manage configuration.
 *
 * Usage: queuectl config get
 *        queuectl config set <key> <value>
 */
@Command(
    name = "config",
    description = "Manage queue configuration",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigGetCommand.class,
        ConfigSetCommand.class
    }
)
public class ConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: queuectl config <get|set> [options]");
        System.out.println("  get             - Show all configuration");
        System.out.println("  set <key> <val> - Set a configuration value");
        System.out.println();
        System.out.println("Configurable keys:");
        System.out.println("  max-retries     - Maximum retry attempts (default: 3)");
        System.out.println("  backoff-base    - Backoff multiplier base (default: 2)");
        System.out.println("  poll-interval   - Worker poll interval in ms (default: 1000)");
        System.out.println("  timeout         - Job timeout in seconds (default: 300)");
        return 0;
    }
}
