package com.queuectl;

import com.queuectl.cli.commands.QueueCTLCommand;
import com.queuectl.cli.config.AppConfig;
import picocli.CommandLine;

/**
 * Main entry point for QueueCTL application.
 * Initializes the database and delegates to Picocli command parsing.
 */
public class Main {

    public static void main(String[] args) {
        try {
            AppConfig.initialize("queue.db");

            int exitCode = new CommandLine(new QueueCTLCommand())
                    .setCaseInsensitiveEnumValuesAllowed(true)
                    .execute(args);

            AppConfig.shutdown();
            System.exit(exitCode);

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            AppConfig.shutdown();
            System.exit(1);
        }
    }
}
