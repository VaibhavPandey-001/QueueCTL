package com.queuectl.service;

import com.queuectl.TestBase;
import com.queuectl.db.DatabaseManager;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigService: get, set, validation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigServiceTest extends TestBase {

    private DatabaseManager dbManager;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        dbManager = createTestDatabase();
        configService = new ConfigService(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @Order(1)
    void testGetDefaultConfig() {
        assertEquals("2", configService.get("base-backoff"));
        assertEquals("3", configService.get("default-max-retry"));
        assertEquals("1000", configService.get("poll-interval"));
        assertEquals("300", configService.get("job-timeout"));
    }

    @Test
    @Order(2)
    void testSetConfig() {
        configService.set("max-retries", "5");
        assertEquals("5", configService.get("default-max-retry"));
    }

    @Test
    @Order(3)
    void testSetBackoffBase() {
        configService.set("backoff-base", "3");
        assertEquals("3", configService.get("base-backoff"));
    }

    @Test
    @Order(4)
    void testSetTimeout() {
        configService.set("timeout", "600");
        assertEquals("600", configService.get("job-timeout"));
    }

    @Test
    @Order(5)
    void testSetInvalidKeyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> configService.set("unknown-key", "value"));
    }

    @Test
    @Order(6)
    void testSetInvalidValueThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> configService.set("max-retries", "abc"));
    }

    @Test
    @Order(7)
    void testSetNegativeValueThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> configService.set("max-retries", "-1"));
    }

    @Test
    @Order(8)
    void testGetAll() {
        Map<String, String> all = configService.getAll();
        assertTrue(all.containsKey("base-backoff"));
        assertTrue(all.containsKey("default-max-retry"));
        assertTrue(all.containsKey("poll-interval"));
        assertTrue(all.containsKey("job-timeout"));
    }

    @Test
    @Order(9)
    void testSetBlankKeyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> configService.set("", "value"));
    }

    @Test
    @Order(10)
    void testSetBlankValueThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> configService.set("max-retries", ""));
    }
}
