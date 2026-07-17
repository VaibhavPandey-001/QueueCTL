package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.service.WorkerService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * CLI command to stop all running workers.
 *
 * Usage: queuectl worker stop
 */
@Command(
    name = "stop",
    description = "Stop all running workers",
    mixinStandardHelpOptions = true
)
public class WorkerStopCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            WorkerService workerService = new WorkerService(AppConfig.getDatabaseManager());
            var runningWorkers = workerService.getRunningWorkers();

            if (runningWorkers.isEmpty()) {
                ConsolePrinter.printInfo("No running workers found.");
                return 0;
            }

            for (var worker : runningWorkers) {
                workerService.stop(worker.getWorkerId());
            }

            ConsolePrinter.printSuccess("Stopped " + runningWorkers.size() + " worker(s).");
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to stop workers: " + e.getMessage());
            return 1;
        }
    }
}
