package com.drhiidden.fsj.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuración de testing basada en environments.
 * 
 * Lee de archivos .properties según el env activo:
 * - test-dev.properties (default)
 * - test-docker.properties
 * - test-prod.properties
 * 
 * Override con system property: -Dtest.env=docker
 */
@Slf4j
@Getter
public class TestConfig {
    
    private static TestConfig instance;
    
    private final String environment;
    private final String baseUrl;
    private final String adminUsername;
    private final String adminPassword;
    private final int timeoutMs;
    private final boolean cleanupAfterTest;
    
    private TestConfig() {
        this.environment = System.getProperty("test.env", "dev");
        
        Properties props = loadProperties("test-" + environment + ".properties");
        
        this.baseUrl = props.getProperty("api.base.url", "http://localhost:8082");
        this.adminUsername = props.getProperty("admin.username", "admin");
        this.adminPassword = props.getProperty("admin.password", "admin123");
        this.timeoutMs = Integer.parseInt(props.getProperty("timeout.ms", "30000"));
        this.cleanupAfterTest = Boolean.parseBoolean(props.getProperty("cleanup.after.test", "true"));
        
        log.info("TestConfig loaded for environment: {}", environment);
        log.info("  Base URL: {}", baseUrl);
        log.info("  Timeout: {}ms", timeoutMs);
        log.info("  Cleanup: {}", cleanupAfterTest);
    }
    
    public static TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }
    
    private Properties loadProperties(String filename) {
        Properties props = new Properties();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                props.load(is);
                log.debug("Loaded properties from {}", filename);
            } else {
                log.warn("Properties file {} not found, using defaults", filename);
            }
        } catch (IOException e) {
            log.error("Error loading properties from {}", filename, e);
        }
        
        return props;
    }
    
    public String getProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
}
