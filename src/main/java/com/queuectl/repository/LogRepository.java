package com.queuectl.repository;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for job log operations.
 */
public class LogRepository {

    private static final Logger logger = LoggerFactory.getLogger(LogRepository.class);
    private final DatabaseManager dbManager;

    public LogRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void insert(JobLog jobLog) {
        String sql = """
            INSERT INTO job_logs (job_id, worker_id, level, message, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, jobLog.getJobId());
            ps.setString(2, jobLog.getWorkerId());
            ps.setString(3, jobLog.getLevel());
            ps.setString(4, jobLog.getMessage());
            ps.setString(5, jobLog.getTimestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert job log for job {}", jobLog.getJobId(), e);
        }
    }

    public List<JobLog> findByJobId(String jobId) {
        String sql = "SELECT * FROM job_logs WHERE job_id = ? ORDER BY timestamp ASC";
        List<JobLog> logs = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find logs for job: " + jobId, e);
        }
        return logs;
    }

    public List<JobLog> findRecent(int limit) {
        String sql = "SELECT * FROM job_logs ORDER BY timestamp DESC LIMIT ?";
        List<JobLog> logs = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find recent logs", e);
        }
        return logs;
    }

    private JobLog mapRow(ResultSet rs) throws SQLException {
        JobLog log = new JobLog();
        log.setLogId(rs.getLong("log_id"));
        log.setJobId(rs.getString("job_id"));
        log.setWorkerId(rs.getString("worker_id"));
        log.setLevel(rs.getString("level"));
        log.setMessage(rs.getString("message"));
        try {
            log.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
        } catch (Exception e) {
            log.setTimestamp(null);
        }
        return log;
    }
}
