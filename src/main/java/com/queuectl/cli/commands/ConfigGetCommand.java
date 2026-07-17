package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.ConfigService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Displays all configuration values.
 *
 * Usage: queuectl config get
 */
@Command(
    name = "get",
    description = "Show all configuration values",
    mixinStandardHelpOptions = true
)
public class ConfigGetCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            ConfigService configService = new ConfigService(AppConfig.getDatabaseManager());
            ConsolePrinter.printConfig(configService.getAll());
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to get config: " + e.getMessage());
            return 1;
        }
    }
}
