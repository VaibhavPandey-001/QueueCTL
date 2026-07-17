package com.queuectl.repository;

import com.queuectl.db.DatabaseManager;
import com.queuectl.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for configuration key-value operations.
 */
public class ConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRepository.class);
    private final DatabaseManager dbManager;

    public ConfigRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public String get(String key) {
        String sql = "SELECT value FROM config WHERE key = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            throw new ConfigurationException("Failed to get config: " + key);
        }
        return null;
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        String sql = "INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            logger.info("Config updated: {} = {}", key, value);
        } catch (SQLException e) {
            throw new ConfigurationException("Failed to set config: " + key + " = " + value);
        }
    }

    public Map<String, String> getAll() {
        Map<String, String> config = new HashMap<>();
        String sql = "SELECT key, value FROM config";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                config.put(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException e) {
            throw new ConfigurationException("Failed to get all config");
        }
        return config;
    }

    public int getBaseBackoff() {
        return getInt("base-backoff", 2);
    }

    public int getDefaultMaxRetry() {
        return getInt("default-max-retry", 3);
    }

    public long getPollIntervalMs() {
        return getLong("poll-interval", 1000);
    }

    public long getJobTimeoutSeconds() {
        return getLong("job-timeout", 300);
    }
}
