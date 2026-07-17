package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.ConfigService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Sets a configuration value.
 *
 * Usage: queuectl config set max-retries 5
 */
@Command(
    name = "set",
    description = "Set a configuration value",
    mixinStandardHelpOptions = true
)
public class ConfigSetCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Configuration key")
    private String key;

    @Parameters(index = "1", description = "Configuration value")
    private String value;

    @Override
    public Integer call() {
        try {
            ConfigService configService = new ConfigService(AppConfig.getDatabaseManager());
            configService.set(key, value);
            ConsolePrinter.printSuccess("Config updated: " + key + " = " + value);
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to set config: " + e.getMessage());
            return 1;
        }
    }
}
