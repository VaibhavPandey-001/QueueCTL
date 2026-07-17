package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.scheduler.Scheduler;
import com.queuectl.utils.ConsolePrinter;
import com.queuectl.worker.WorkerManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command to start workers.
 *
 * Usage: queuectl worker start [--count N]
 */
@Command(
    name = "start",
    description = "Start queue workers",
    mixinStandardHelpOptions = true
)
public class WorkerStartCommand implements Callable<Integer> {

    @Option(names = {"-c", "--count"}, description = "Number of workers to start (default: 1)", defaultValue = "1")
    private int count;

    @Override
    public Integer call() {
        try {
            WorkerManager workerManager = new WorkerManager(AppConfig.getDatabaseManager());
            Scheduler scheduler = new Scheduler(AppConfig.getDatabaseManager());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ConsolePrinter.printInfo("Shutting down workers...");
                scheduler.stop();
                workerManager.stopAllWorkers();
                AppConfig.shutdown();
                ConsolePrinter.printInfo("Shutdown complete.");
            }));

            scheduler.start();
            ConsolePrinter.printInfo("Starting " + count + " worker(s)...");
            workerManager.startWorkers(count);

            ConsolePrinter.printSuccess("Workers started. Press Ctrl+C to stop.");
            Thread.currentThread().join();
            return 0;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ConsolePrinter.printInfo("Worker interrupted.");
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to start workers: " + e.getMessage());
            return 1;
        }
    }
}
