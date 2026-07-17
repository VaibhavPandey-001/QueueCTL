package com.queuectl.repository;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for job CRUD operations and job selection queries.
 */
public class JobRepository {

    private static final Logger logger = LoggerFactory.getLogger(JobRepository.class);
    private final DatabaseManager dbManager;

    public JobRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void insert(Job job) {
        String sql = """
            INSERT INTO jobs (id, command, state, attempts, max_retries, created_at, updated_at,
                next_retry_at, last_error, worker_id, locked, priority, run_at, output)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getCommand());
            ps.setString(3, job.getState().name());
            ps.setInt(4, job.getAttempts());
            ps.setInt(5, job.getMaxRetries());
            ps.setString(6, job.getCreatedAt() != null ? job.getCreatedAt().toString() : LocalDateTime.now().toString());
            ps.setString(7, job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : LocalDateTime.now().toString());
            ps.setString(8, job.getNextRetryAt() != null ? job.getNextRetryAt().toString() : null);
            ps.setString(9, job.getLastError());
            ps.setString(10, job.getWorkerId());
            ps.setInt(11, job.isLocked() ? 1 : 0);
            ps.setInt(12, job.getPriority());
            ps.setString(13, job.getRunAt() != null ? job.getRunAt().toString() : null);
            ps.setString(14, job.getOutput());
            ps.executeUpdate();
            logger.debug("Inserted job: {}", job.getId());
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to insert job: " + job.getId(), e);
        }
    }

    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM jobs WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to check job existence: " + id, e);
        }
    }

    public Optional<Job> findById(String id) {
        String sql = "SELECT * FROM jobs WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find job: " + id, e);
        }
        return Optional.empty();
    }

    public List<Job> findAll() {
        String sql = "SELECT * FROM jobs ORDER BY priority DESC, created_at ASC";
        return queryList(sql);
    }

    public List<Job> findByState(JobState state) {
        String sql = "SELECT * FROM jobs WHERE state = ? ORDER BY priority DESC, created_at ASC";
        List<Job> jobs = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, state.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find jobs by state", e);
        }
        return jobs;
    }

    /**
     * Attempts to claim a pending job using a serialized transaction.
     * Uses setAutoCommit(false) to start a transaction, then commits on success.
     * Returns the job if successfully locked, null otherwise.
     */
    public Optional<Job> claimJob(String workerId) {
        Connection conn = dbManager.getConnection();
        boolean origAutoCommit = true;
        try {
            origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String selectSql = """
                SELECT * FROM jobs
                WHERE state = 'PENDING' AND locked = 0
                AND (run_at IS NULL OR run_at <= ?)
                AND (next_retry_at IS NULL OR next_retry_at <= ?)
                ORDER BY priority DESC, created_at ASC
                LIMIT 1
            """;

            String now = LocalDateTime.now().toString();
            Job job = null;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, now);
                ps.setString(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        job = mapRow(rs);
                    }
                }
            }

            if (job == null) {
                conn.commit();
                conn.setAutoCommit(origAutoCommit);
                return Optional.empty();
            }

            String updateSql = """
                UPDATE jobs SET locked = 1, worker_id = ?, state = 'PROCESSING', updated_at = ?
                WHERE id = ? AND locked = 0
            """;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, workerId);
                ps.setString(2, LocalDateTime.now().toString());
                ps.setString(3, job.getId());
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    conn.setAutoCommit(origAutoCommit);
                    return Optional.empty();
                }
            }

            conn.commit();
            conn.setAutoCommit(origAutoCommit);

            job.setLocked(true);
            job.setWorkerId(workerId);
            job.setState(JobState.PROCESSING);
            job.setUpdatedAt(LocalDateTime.now());
            logger.info("Job {} claimed by worker {}", job.getId(), workerId);
            return Optional.of(job);

        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(origAutoCommit);
            } catch (SQLException ex) {
                logger.error("Failed to rollback", ex);
            }
            logger.error("Failed to claim job for worker {}", workerId, e);
            return Optional.empty();
        }
    }

    public void updateState(String jobId, JobState state) {
        String sql = "UPDATE jobs SET state = ?, updated_at = ?, locked = 0 WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, state.name());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to update job state: " + jobId, e);
        }
    }

    public void updateCompleted(String jobId, String output) {
        String sql = "UPDATE jobs SET state = ?, output = ?, updated_at = ?, worker_id = NULL, locked = 0 WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, JobState.COMPLETED.name());
            ps.setString(2, output);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setString(4, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to mark job completed: " + jobId, e);
        }
    }

    public void updateFailed(String jobId, int attempts, String lastError, String output, LocalDateTime nextRetryAt) {
        String sql = """
            UPDATE jobs SET state = ?, attempts = ?, last_error = ?, output = ?,
                next_retry_at = ?, updated_at = ?, locked = 0, worker_id = NULL
            WHERE id = ?
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, JobState.FAILED.name());
            ps.setInt(2, attempts);
            ps.setString(3, lastError);
            ps.setString(4, output);
            ps.setString(5, nextRetryAt != null ? nextRetryAt.toString() : null);
            ps.setString(6, LocalDateTime.now().toString());
            ps.setString(7, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to mark job failed: " + jobId, e);
        }
    }

    public void updateDead(String jobId, int attempts, String lastError, String output) {
        String sql = """
            UPDATE jobs SET state = ?, attempts = ?, last_error = ?, output = ?,
                updated_at = ?, locked = 0, worker_id = NULL
            WHERE id = ?
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, JobState.DEAD.name());
            ps.setInt(2, attempts);
            ps.setString(3, lastError);
            ps.setString(4, output);
            ps.setString(5, LocalDateTime.now().toString());
            ps.setString(6, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to mark job dead: " + jobId, e);
        }
    }

    public void moveToDlq(String jobId, int attempts, String lastError, String output) {
        updateDead(jobId, attempts, lastError, output);
        logger.warn("Job {} moved to Dead Letter Queue after {} attempts", jobId, attempts);
    }

    public void retryFromDlq(String jobId) {
        String sql = """
            UPDATE jobs SET state = 'PENDING', attempts = 0, last_error = NULL,
                locked = 0, worker_id = NULL, next_retry_at = NULL, updated_at = ?
            WHERE id = ? AND state = 'DEAD'
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, jobId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new com.queuectl.exception.JobNotFoundException(jobId);
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to retry job from DLQ: " + jobId, e);
        }
    }

    public void unlockJob(String jobId) {
        String sql = "UPDATE jobs SET locked = 0, worker_id = NULL, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to unlock job: " + jobId, e);
        }
    }

    public void cleanupStaleLocks() {
        String sql = """
            UPDATE jobs SET locked = 0, worker_id = NULL, state = 'PENDING', updated_at = ?
            WHERE locked = 1 AND worker_id NOT IN (SELECT worker_id FROM workers WHERE status = 'RUNNING')
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            int count = ps.executeUpdate();
            if (count > 0) {
                logger.warn("Cleaned up {} stale job locks", count);
            }
        } catch (SQLException e) {
            logger.error("Failed to clean up stale locks", e);
        }
    }

    public void delete(String jobId) {
        String sql = "DELETE FROM jobs WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to delete job: " + jobId, e);
        }
    }

    public int countByState(JobState state) {
        String sql = "SELECT COUNT(*) FROM jobs WHERE state = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, state.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to count jobs", e);
        }
        return 0;
    }

    public double getAverageExecutionTime() {
        String sql = """
            SELECT AVG(CAST((julianday(updated_at) - julianday(created_at)) * 86400 AS INTEGER))
            FROM jobs WHERE state = 'COMPLETED'
        """;
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.error("Failed to get average execution time", e);
        }
        return 0.0;
    }

    /**
     * Moves FAILED jobs whose next_retry_at has passed back to PENDING state.
     * This allows workers to pick them up for retry.
     *
     * @return number of jobs reactivated
     */
    public int reactivateRetriableJobs() {
        String sql = """
            UPDATE jobs SET state = 'PENDING', updated_at = ?
            WHERE state = 'FAILED' AND locked = 0
            AND next_retry_at IS NOT NULL AND next_retry_at <= ?
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, now);
            ps.setString(2, now);
            int count = ps.executeUpdate();
            if (count > 0) {
                logger.info("Reactivated {} retriable jobs", count);
            }
            return count;
        } catch (SQLException e) {
            logger.error("Failed to reactivate retriable jobs", e);
            return 0;
        }
    }

    private List<Job> queryList(String sql) {
        List<Job> jobs = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                jobs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to query jobs", e);
        }
        return jobs;
    }

    private Job mapRow(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setId(rs.getString("id"));
        job.setCommand(rs.getString("command"));
        job.setState(JobState.valueOf(rs.getString("state")));
        job.setAttempts(rs.getInt("attempts"));
        job.setMaxRetries(rs.getInt("max_retries"));
        job.setCreatedAt(parseDateTime(rs.getString("created_at")));
        job.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        job.setNextRetryAt(parseDateTime(rs.getString("next_retry_at")));
        job.setLastError(rs.getString("last_error"));
        job.setWorkerId(rs.getString("worker_id"));
        job.setLocked(rs.getInt("locked") == 1);
        job.setPriority(rs.getInt("priority"));
        job.setRunAt(parseDateTime(rs.getString("run_at")));
        job.setOutput(rs.getString("output"));
        return job;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
