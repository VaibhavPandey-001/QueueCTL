package com.queuectl.service;

import com.queuectl.db.DatabaseManager;
import com.queuectl.repository.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Service for configuration management.
 */
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private final ConfigRepository configRepository;

    public ConfigService(DatabaseManager dbManager) {
        this.configRepository = new ConfigRepository(dbManager);
    }

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * Gets a configuration value by key.
     */
    public String get(String key) {
        return configRepository.get(key);
    }

    /**
     * Sets a configuration value. Validates key and value.
     */
    public void set(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Config key cannot be null or blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Config value cannot be null or blank");
        }

        String normalizedKey = normalizeKey(key);

        try {
            switch (normalizedKey) {
                case "base-backoff", "backoff-base" -> {
                    int v = Integer.parseInt(value);
                    if (v < 1) throw new IllegalArgumentException("base-backoff must be >= 1");
                    configRepository.set("base-backoff", String.valueOf(v));
                }
                case "default-max-retry", "max-retries" -> {
                    int v = Integer.parseInt(value);
                    if (v < 0) throw new IllegalArgumentException("default-max-retry must be >= 0");
                    configRepository.set("default-max-retry", String.valueOf(v));
                }
                case "poll-interval" -> {
                    long v = Long.parseLong(value);
                    if (v < 100) throw new IllegalArgumentException("poll-interval must be >= 100ms");
                    configRepository.set("poll-interval", String.valueOf(v));
                }
                case "job-timeout", "timeout" -> {
                    long v = Long.parseLong(value);
                    if (v < 1) throw new IllegalArgumentException("job-timeout must be >= 1");
                    configRepository.set("job-timeout", String.valueOf(v));
                }
                default -> throw new IllegalArgumentException("Unknown config key: " + key);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value for " + key + ": " + value);
        }

        logger.info("Config updated: {} = {}", normalizedKey, value);
    }

    /**
     * Returns all configuration entries.
     */
    public Map<String, String> getAll() {
        return configRepository.getAll();
    }

    /**
     * Normalizes key aliases to canonical form.
     */
    private String normalizeKey(String key) {
        return switch (key.toLowerCase().trim()) {
            case "backoff-base" -> "base-backoff";
            case "max-retries" -> "default-max-retry";
            case "timeout" -> "job-timeout";
            default -> key.toLowerCase().trim();
        };
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
