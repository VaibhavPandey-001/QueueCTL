package com.queuectl.cli.commands;

import com.queuectl.cli.config.AppConfig;
import com.queuectl.entity.JobState;
import com.queuectl.service.JobService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command to list jobs with optional state filtering.
 *
 * Usage: queuectl list [--state pending]
 */
@Command(
    name = "list",
    description = "List all jobs in the queue",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Option(names = {"-s", "--state"}, description = "Filter by state: pending, processing, completed, failed, dead")
    private String stateFilter;

    @Override
    public Integer call() {
        try {
            JobService jobService = new JobService(AppConfig.getDatabaseManager());

            if (stateFilter != null && !stateFilter.isBlank()) {
                JobState state = JobState.valueOf(stateFilter.toUpperCase());
                ConsolePrinter.printJobList(jobService.listJobsByState(state));
            } else {
                ConsolePrinter.printJobList(jobService.listJobs());
            }
            return 0;
        } catch (IllegalArgumentException e) {
            ConsolePrinter.printError("Invalid state: " + stateFilter + ". Valid states: pending, processing, completed, failed, dead");
            return 1;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to list jobs: " + e.getMessage());
            return 1;
        }
    }
}
