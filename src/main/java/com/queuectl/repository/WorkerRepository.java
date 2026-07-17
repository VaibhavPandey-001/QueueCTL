package com.queuectl.repository;

import com.queuectl.db.DatabaseManager;
import com.queuectl.entity.WorkerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for worker registration and heartbeat operations.
 */
public class WorkerRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkerRepository.class);
    private final DatabaseManager dbManager;

    public WorkerRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void register(WorkerInfo worker) {
        String sql = """
            INSERT OR REPLACE INTO workers (worker_id, status, started_at, last_heartbeat, pid)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, worker.getWorkerId());
            ps.setString(2, worker.getStatus());
            ps.setString(3, worker.getStartedAt().toString());
            ps.setString(4, worker.getLastHeartbeat().toString());
            ps.setLong(5, worker.getPid());
            ps.executeUpdate();
            logger.info("Worker registered: {}", worker.getWorkerId());
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to register worker", e);
        }
    }

    public void updateHeartbeat(String workerId) {
        String sql = "UPDATE workers SET last_heartbeat = ? WHERE worker_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, workerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update heartbeat for worker {}", workerId, e);
        }
    }

    public void updateStatus(String workerId, String status) {
        String sql = "UPDATE workers SET status = ?, last_heartbeat = ? WHERE worker_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, workerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update status for worker {}", workerId, e);
        }
    }

    public void unregister(String workerId) {
        String sql = "DELETE FROM workers WHERE worker_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, workerId);
            ps.executeUpdate();
            logger.info("Worker unregistered: {}", workerId);
        } catch (SQLException e) {
            logger.error("Failed to unregister worker {}", workerId, e);
        }
    }

    public List<WorkerInfo> findAllRunning() {
        String sql = "SELECT * FROM workers WHERE status = 'RUNNING' ORDER BY started_at ASC";
        List<WorkerInfo> workers = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                workers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find running workers", e);
        }
        return workers;
    }

    public List<WorkerInfo> findAll() {
        String sql = "SELECT * FROM workers ORDER BY started_at ASC";
        List<WorkerInfo> workers = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                workers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new com.queuectl.exception.DatabaseException("Failed to find workers", e);
        }
        return workers;
    }

    public int countRunning() {
        String sql = "SELECT COUNT(*) FROM workers WHERE status = 'RUNNING'";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count running workers", e);
        }
        return 0;
    }

    public void cleanupStaleWorkers(long staleThresholdMinutes) {
        String sql = """
            DELETE FROM workers
            WHERE status = 'RUNNING' AND last_heartbeat < ?
        """;
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            String threshold = LocalDateTime.now().minusMinutes(staleThresholdMinutes).toString();
            ps.setString(1, threshold);
            int count = ps.executeUpdate();
            if (count > 0) {
                logger.warn("Cleaned up {} stale workers", count);
            }
        } catch (SQLException e) {
            logger.error("Failed to cleanup stale workers", e);
        }
    }

    private WorkerInfo mapRow(ResultSet rs) throws SQLException {
        WorkerInfo w = new WorkerInfo();
        w.setWorkerId(rs.getString("worker_id"));
        w.setStatus(rs.getString("status"));
        w.setStartedAt(parseDateTime(rs.getString("started_at")));
        w.setLastHeartbeat(parseDateTime(rs.getString("last_heartbeat")));
        w.setPid(rs.getLong("pid"));
        return w;
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
