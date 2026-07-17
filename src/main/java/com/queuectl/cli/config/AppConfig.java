package com.queuectl.cli.config;

import com.queuectl.db.DatabaseManager;

/**
 * Application-wide configuration holder.
 * Holds the DatabaseManager instance shared across commands.
 */
public class AppConfig {

    private static final String DEFAULT_DB_PATH = "queue.db";
    private static DatabaseManager databaseManager;

    private AppConfig() {}

    /**
     * Initializes the application configuration and database.
     */
    public static void initialize(String dbPath) {
        String path = dbPath != null ? dbPath : DEFAULT_DB_PATH;
        databaseManager = new DatabaseManager(path);
        databaseManager.initialize();
    }

    /**
     * Returns the shared DatabaseManager instance.
     */
    public static DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            throw new IllegalStateException("AppConfig not initialized. Call initialize() first.");
        }
        return databaseManager;
    }

    /**
     * Closes the database connection.
     */
    public static void shutdown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
