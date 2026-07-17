package com.queuectl;

import com.queuectl.db.DatabaseManager;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for tests that need a fresh in-memory or temp SQLite database.
 */
public abstract class TestBase {

    private String lastDbPath;

    protected DatabaseManager createTestDatabase() {
        try {
            Path tempDir = Files.createTempDirectory("queuectl-test");
            lastDbPath = tempDir.resolve("test-queue.db").toString();
            DatabaseManager dbManager = new DatabaseManager(lastDbPath);
            dbManager.initialize();
            return dbManager;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test database", e);
        }
    }

    protected DatabaseManager reopenTestDatabase() {
        try {
            DatabaseManager dbManager = new DatabaseManager(lastDbPath);
            dbManager.initialize();
            return dbManager;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reopen test database", e);
        }
    }
}
