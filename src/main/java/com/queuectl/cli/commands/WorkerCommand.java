package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.db.DatabaseManager;
import com.queuectl.service.JobService;
import com.queuectl.service.WorkerService;
import com.queuectl.service.RetryService;
import com.queuectl.scheduler.Scheduler;
import com.queuectl.utils.ConsolePrinter;
import com.queuectl.worker.WorkerManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CLI command to manage workers (start/stop).
 */
@Command(
    name = "worker",
    description = "Manage queue workers",
    mixinStandardHelpOptions = true,
    subcommands = {
        WorkerStartCommand.class,
        WorkerStopCommand.class
    }
)
public class WorkerCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: queuectl worker <start|stop> [options]");
        System.out.println("  start  - Start one or more workers");
        System.out.println("  stop   - Stop all running workers");
        return 0;
    }
}
