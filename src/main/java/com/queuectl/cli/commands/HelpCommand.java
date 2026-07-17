package com.queuectl.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Displays detailed help information for all commands.
 */
@Command(
    name = "help",
    description = "Show detailed help information",
    mixinStandardHelpOptions = true
)
public class HelpCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("""
            ╔══════════════════════════════════════════════════════════════╗
            ║                  QueueCTL - Help Guide                     ║
            ╚══════════════════════════════════════════════════════════════╝

            COMMANDS:

            enqueue <json>
              Enqueue a new job. Provide a JSON object with id, command, and optional fields.
              Example: queuectl enqueue '{"id":"job1","command":"echo hello"}'
              Options: priority, maxRetries, runAt (ISO datetime)

            worker start [--count N]
              Start one or more background workers that process jobs.
              Workers poll the database, claim jobs, and execute them.
              Example: queuectl worker start --count 5

            worker stop
              Gracefully stop all running workers. Workers finish current jobs first.

            list [--state <state>]
              List all jobs or filter by state.
              States: pending, processing, completed, failed, dead
              Example: queuectl list --state pending

            status
              Display system status: job counts, active workers, configuration.

            dlq list
              Show all jobs in the Dead Letter Queue (state=DEAD).

            dlq retry <jobId>
              Move a dead job back to PENDING. Resets attempts and clears errors.
              Example: queuectl dlq retry job1

            config get
              Show all configuration values.

            config set <key> <value>
              Update a configuration value. Changes persist across restarts.
              Keys:
                max-retries   - Maximum retry attempts (default: 3)
                backoff-base  - Exponential backoff base (default: 2)
                poll-interval - Worker poll interval in ms (default: 1000)
                timeout       - Job timeout in seconds (default: 300)

            help
              Show this help guide.

            ARCHITECTURE:

            Jobs flow through states:
              PENDING -> PROCESSING -> COMPLETED
                                    -> FAILED -> PENDING (retry)
                                              -> DEAD (max retries exceeded)

            Workers:
              - Poll DB every poll-interval ms
              - Claim jobs using BEGIN IMMEDIATE transaction
              - Execute commands via ProcessBuilder
              - Retry with exponential backoff: delay = base^attempt

            Persistence:
              All data stored in SQLite (queue.db).
              Survives application restarts.

            Locking:
              SQLite transactions prevent duplicate job execution.
              Stale locks are cleaned up automatically.
            """);
        return 0;
    }
}
