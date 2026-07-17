package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.StatusService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * CLI command to display system status and metrics.
 *
 * Usage: queuectl status
 */
@Command(
    name = "status",
    description = "Display queue system status",
    mixinStandardHelpOptions = true
)
public class StatusCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            StatusService statusService = new StatusService(AppConfig.getDatabaseManager());
            ConsolePrinter.printStatus(statusService.getStatus());
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to get status: " + e.getMessage());
            return 1;
        }
    }
}
