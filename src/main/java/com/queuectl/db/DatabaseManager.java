package com.queuectl.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection and schema initialization.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL_PREFIX = "jdbc:sqlite:";
    private final String dbPath;
    private Connection connection;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Opens the database connection and initializes the schema.
     */
    public synchronized void initialize() {
        try {
            connection = DriverManager.getConnection(DB_URL_PREFIX + dbPath);
            connection.setAutoCommit(true);
            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");
            }
            logger.info("Database connected: {}", dbPath);
            createTables();
            insertDefaultConfig();
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new com.queuectl.exception.DatabaseException("Failed to initialize database", e);
        }
    }

    /**
     * Returns the current database connection.
     */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL_PREFIX + dbPath);
                connection.setAutoCommit(true);
                try (var stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA busy_timeout = 5000");
                }
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to get database connection", e);
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS jobs (
                    id TEXT PRIMARY KEY,
                    command TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT 'PENDING',
                    attempts INTEGER NOT NULL DEFAULT 0,
                    max_retries INTEGER NOT NULL DEFAULT 3,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    next_retry_at TEXT,
                    last_error TEXT,
                    worker_id TEXT,
                    locked INTEGER NOT NULL DEFAULT 0,
                    priority INTEGER NOT NULL DEFAULT 0,
                    run_at TEXT,
                    output TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS workers (
                    worker_id TEXT PRIMARY KEY,
                    status TEXT NOT NULL DEFAULT 'RUNNING',
                    started_at TEXT NOT NULL,
                    last_heartbeat TEXT NOT NULL,
                    pid INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS config (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS job_logs (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id TEXT NOT NULL,
                    worker_id TEXT,
                    level TEXT NOT NULL,
                    message TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (job_id) REFERENCES jobs(id)
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_jobs_state_locked
                ON jobs(state, locked, priority DESC, created_at ASC)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_jobs_next_retry
                ON jobs(next_retry_at)
            """);

            logger.info("Database tables created/verified");
        }
    }

    private void insertDefaultConfig() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO config (key, value) VALUES ('base-backoff', '2')
            """);
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO config (key, value) VALUES ('default-max-retry', '3')
            """);
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO config (key, value) VALUES ('poll-interval', '1000')
            """);
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO config (key, value) VALUES ('job-timeout', '300')
            """);
        }
    }
}
