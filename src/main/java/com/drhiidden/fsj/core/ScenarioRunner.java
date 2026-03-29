package com.drhiidden.fsj.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.Map;

/**
 * Clase base para todos los escenarios de testing.
 * 
 * Proporciona:
 * - ApiClient configurado según environment
 * - ScenarioContext para compartir datos
 * - Lifecycle hooks (setup/cleanup)
 * - Logging estructurado
 */
@Slf4j
public abstract class ScenarioRunner {
    
    protected ApiClient api;
    protected ScenarioContext context;
    protected TestConfig config;
    
    @BeforeEach
    void baseSetUp(TestInfo testInfo) {
        config = TestConfig.getInstance();
        api = new ApiClient(config.getBaseUrl());
        context = new ScenarioContext(testInfo.getDisplayName());
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("▶ SCENARIO: {}", testInfo.getDisplayName());
        log.info("  Environment: {}", config.getEnvironment());
        log.info("  Base URL: {}", config.getBaseUrl());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        context.startTiming("scenario-total");
        
        // Hook para subclases
        setUp();
    }
    
    @AfterEach
    void baseTearDown(TestInfo testInfo) {
        long duration = context.endTiming("scenario-total");
        
        // Hook para subclases
        tearDown();
        
        if (config.isCleanupAfterTest()) {
            cleanup();
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✓ SCENARIO COMPLETED: {} ({}ms)", testInfo.getDisplayName(), duration);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    /**
     * Hook: setup específico del escenario (crear datos, autenticarse, etc.).
     */
    protected void setUp() {
        // Override en subclases si necesario
    }
    
    /**
     * Hook: teardown específico del escenario.
     */
    protected void tearDown() {
        // Override en subclases si necesario
    }
    
    /**
     * Hook: limpiar recursos creados durante el test.
     */
    protected void cleanup() {
        // Override en subclases para eliminar datos de prueba
    }
    
    /**
     * Helper: login y obtener token JWT.
     */
    protected String loginAsAdmin() {
        log.info("  [Auth] Logging in as admin...");
        
        var response = api.post("/api/auth/login")
            .withBody(Map.of(
                "username", config.getAdminUsername(),
                "password", config.getAdminPassword()
            ))
            .expectStatus(200)
            .execute();
        
        String token = response.extract("token");
        context.store("admin-token", token);
        
        log.info("  [Auth] ✓ Token obtained");
        return token;
    }
    
    /**
     * Helper: wait for condition con timeout.
     */
    protected void waitFor(String description, java.util.function.Supplier<Boolean> condition) {
        log.info("  [Wait] {}", description);
        
        int attempts = 0;
        int maxAttempts = config.getTimeoutMs() / 1000;
        
        while (attempts < maxAttempts) {
            if (condition.get()) {
                log.info("  [Wait] ✓ Condition met after {}s", attempts);
                return;
            }
            
            try {
                Thread.sleep(1000);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted", e);
            }
        }
        
        throw new AssertionError("Condition not met after " + maxAttempts + "s: " + description);
    }
    
    /**
     * Helper: Sleep for specified milliseconds.
     * Útil para esperar DB commits o evitar race conditions.
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
    
    /**
     * Helper: Wait después de DB write operations para asegurar commit.
     * Usa esto después de POST/PUT/DELETE antes de verificar con GET.
     */
    protected void waitForDbCommit() {
        sleep(200); // 200ms suficiente para MySQL commit + cache invalidation
    }
}
