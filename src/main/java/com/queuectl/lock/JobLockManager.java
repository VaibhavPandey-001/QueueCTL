package com.queuectl.lock;

import com.queuectl.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Provides pessimistic locking via SQLite transactions.
 * Uses BEGIN IMMEDIATE to prevent concurrent access to the same job.
 */
public class JobLockManager {

    private static final Logger logger = LoggerFactory.getLogger(JobLockManager.class);
    private final DatabaseManager dbManager;

    public JobLockManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Attempts to acquire a lock on a job.
     * Uses BEGIN IMMEDIATE transaction to serialize access.
     *
     * @param jobId the job to lock
     * @param workerId the worker requesting the lock
     * @return true if lock was acquired successfully
     */
    public boolean tryLock(String jobId, String workerId) {
        Connection conn = dbManager.getConnection();
        boolean origAutoCommit = true;
        try {
            origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String sql = "UPDATE jobs SET locked = 1, worker_id = ? WHERE id = ? AND locked = 0";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, workerId);
                ps.setString(2, jobId);
                int updated = ps.executeUpdate();
                conn.commit();
                conn.setAutoCommit(origAutoCommit);

                if (updated > 0) {
                    logger.debug("Lock acquired for job {} by worker {}", jobId, workerId);
                    return true;
                } else {
                    logger.debug("Lock failed for job {} - already locked", jobId);
                    return false;
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(origAutoCommit);
            } catch (SQLException ex) {
                logger.error("Failed to rollback after lock failure", ex);
            }
            logger.error("Failed to acquire lock for job {}", jobId, e);
            return false;
        }
    }

    /**
     * Releases a lock on a job.
     *
     * @param jobId the job to unlock
     */
    public void releaseLock(String jobId) {
        String sql = "UPDATE jobs SET locked = 0, worker_id = NULL WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
            logger.debug("Lock released for job {}", jobId);
        } catch (SQLException e) {
            logger.error("Failed to release lock for job {}", jobId, e);
        }
    }
}
