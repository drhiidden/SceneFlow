package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Suite regresiva: verifica que TODOS los endpoints públicos responden.
 * 
 * Útil para:
 * - Smoke testing después de deploy
 * - Detectar endpoints rotos
 * - Validar backward compatibility
 */
@Slf4j
@DisplayName("Regression Smoke Test Suite")
class RegressionSmokeTest extends ScenarioRunner {
    
    @Test
    @DisplayName("All public GET endpoints should respond 200 or 404 (not 500)")
    void allPublicEndpoints_doNotCrash() {
        List<String> endpoints = List.of(
            "/actuator/health",
            "/api/artists",
            "/api/artists/1",
            "/api/albums",
            "/api/songs",
            "/api/news",
            "/api/news/featured",
            "/api/news/recent",
            "/api/cms/hero-carousel/active",
            "/api/events",
            "/api/battles"
        );
        
        List<String> failures = new ArrayList<>();
        
        for (String endpoint : endpoints) {
            try {
                context.startTiming(endpoint);
                var response = api.get(endpoint).execute();
                context.endTiming(endpoint);
                
                int status = response.getStatus();
                
                // Permitir 200 (OK), 404 (Not Found), 401 (Auth required)
                // Fallar en 500+ (server errors)
                if (status >= 500) {
                    failures.add(String.format("%s returned %d (SERVER ERROR)", endpoint, status));
                    log.error("  ✗ {} crashed with {}", endpoint, status);
                } else {
                    log.info("  ✓ {} responded with {}", endpoint, status);
                }
                
            } catch (Exception e) {
                failures.add(endpoint + " threw exception: " + e.getMessage());
                log.error("  ✗ {} threw exception", endpoint, e);
            }
        }
        
        if (!failures.isEmpty()) {
            throw new AssertionError("Endpoints crashed:\n  - " + String.join("\n  - ", failures));
        }
        
        log.info("✓ All {} endpoints responded without server errors", endpoints.size());
    }
    
    @Test
    @DisplayName("Core endpoints return expected structure")
    void coreEndpoints_returnValidStructure() {
        // News pagination
        api.get("/api/news")
            .expectStatus(200)
            .execute()
            .assertFieldExists("content")
            .assertFieldExists("totalElements")
            .assertFieldExists("pageable");
        
        // Featured news array
        api.get("/api/news/featured")
            .expectStatus(200)
            .execute()
            .assertFieldExists("$");
        
        // Active carousel array
        api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .assertFieldExists("$");
        
        log.info("✓ All core endpoints return expected structure");
    }
    
    @Test
    @DisplayName("Pagination works consistently across endpoints")
    void pagination_worksConsistently() {
        String[] paginatedEndpoints = {
            "/api/artists",
            "/api/albums",
            "/api/songs",
            "/api/news"
        };
        
        for (String endpoint : paginatedEndpoints) {
            api.get(endpoint)
                .withQuery("page", 0)
                .withQuery("size", 5)
                .expectStatus(200)
                .execute()
                .assertFieldExists("content")
                .assertFieldExists("totalElements")
                .assertFieldExists("pageable.pageNumber")
                .assertField("pageable.pageSize", 5)
                .assertField("number", 0);
            
            log.info("  ✓ {} supports pagination", endpoint);
        }
    }
    
    @Test
    @DisplayName("Search functionality works across entities")
    void search_worksAcrossEntities() {
        // Verificar si search está implementado (puede retornar 404/501)
        var artistsSearchResponse = api.get("/api/artists/search")
            .withQuery("query", "test")
            .execute();
        
        int status = artistsSearchResponse.getStatus();
        if (status == 200) {
            artistsSearchResponse.assertFieldExists("$");
            log.info("  ✓ Artists search functional");
        } else if (status == 404 || status == 501) {
            log.warn("  ⚠ Artists search not implemented (status: {})", status);
        } else if (status >= 500) {
            throw new AssertionError("Artists search crashed with " + status);
        }
        
        // Songs search (si existe)
        var songsSearchResponse = api.get("/api/songs/search")
            .withQuery("title", "test")
            .execute();
        
        int songsStatus = songsSearchResponse.getStatus();
        if (songsStatus == 200) {
            songsSearchResponse.assertFieldExists("$");
            log.info("  ✓ Songs search functional");
        } else if (songsStatus == 404 || songsStatus == 501) {
            log.warn("  ⚠ Songs search not implemented (status: {})", songsStatus);
        } else if (songsStatus >= 500) {
            throw new AssertionError("Songs search crashed with " + songsStatus);
        }
        
        log.info("✓ Search endpoints validated (no crashes)");
    }
}
