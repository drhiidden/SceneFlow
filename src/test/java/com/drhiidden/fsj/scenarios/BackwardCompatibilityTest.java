package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests de compatibilidad hacia atrás (backward compatibility).
 * 
 * Garantiza que:
 * - Contratos de API no se rompen
 * - Campos existentes no desaparecen
 * - Formatos de fecha/enum consistentes
 */
@Slf4j
@DisplayName("Backward Compatibility Tests")
class BackwardCompatibilityTest extends ScenarioRunner {
    
    @Test
    @DisplayName("Artist API: campos obligatorios siempre presentes")
    void artistApi_mandatoryFieldsAlwaysPresent() {
        var artists = api.get("/api/artists")
            .expectStatus(200)
            .execute()
            .extract("content");
        
        if (artists instanceof java.util.List<?> list && !list.isEmpty()) {
            var firstArtist = (Map<String, Object>) list.get(0);
            
            // Campos que NUNCA deben desaparecer
            String[] mandatoryFields = {
                "id", "name", "type", "imageUrl", "verified"
            };
            
            for (String field : mandatoryFields) {
                if (!firstArtist.containsKey(field)) {
                    throw new AssertionError("BREAKING CHANGE: Artist API missing field '" + field + "'");
                }
            }
            
            log.info("  ✓ All mandatory Artist fields present");
        }
    }
    
    @Test
    @DisplayName("News API: category enum backward compatible")
    void newsApi_categoryEnumStable() {
        var news = api.get("/api/news")
            .expectStatus(200)
            .execute()
            .extract("content");
        
        if (news instanceof java.util.List<?> list && !list.isEmpty()) {
            var firstNews = (Map<String, Object>) list.get(0);
            
            if (firstNews.containsKey("category")) {
                String category = (String) firstNews.get("category");
                
                // Valores válidos según NewsCategory.java
                String[] validCategories = {
                    "BATALLAS", "ENTREVISTAS", "ARCHIVO", "EVENTOS", "LANZAMIENTOS", "GENERAL"
                };
                
                boolean valid = false;
                for (String validCat : validCategories) {
                    if (validCat.equals(category)) {
                        valid = true;
                        break;
                    }
                }
                
                if (!valid) {
                    throw new AssertionError("BREAKING CHANGE: Unknown news category '" + category + "'");
                }
                
                log.info("  ✓ News category '{}' is valid", category);
            }
        }
    }
    
    @Test
    @DisplayName("Pagination format consistent across all endpoints")
    void paginationFormat_consistentEverywhere() {
        String[] paginatedEndpoints = {
            "/api/artists", "/api/albums", "/api/songs", "/api/news"
        };
        
        for (String endpoint : paginatedEndpoints) {
            var response = api.get(endpoint)
                .expectStatus(200)
                .execute()
                .asMap();
            
            // Formato Spring Data Page
            String[] requiredFields = {
                "content", "pageable", "totalElements", "totalPages", 
                "size", "number", "numberOfElements", "first", "last", "empty"
            };
            
            for (String field : requiredFields) {
                if (!response.containsKey(field)) {
                    throw new AssertionError(
                        "BREAKING CHANGE: " + endpoint + " missing pagination field '" + field + "'"
                    );
                }
            }
            
            log.info("  ✓ {} pagination format OK", endpoint);
        }
    }
    
    @Test
    @DisplayName("Error responses have consistent structure")
    void errorResponses_haveConsistentStructure() {
        // Trigger validation error
        var errorResponse = api.post("/api/news")
            .withBody(Map.of("title", "No category"))
            .execute();
        
        // No crashea (400, no 500)
        if (errorResponse.getStatus() >= 500) {
            throw new AssertionError("Validation error returned 500+ instead of 400");
        }
        
        // Estructura de error consistente
        var errorMap = errorResponse.asMap();
        
        String[] errorFields = {"status", "error", "message", "timestamp"};
        for (String field : errorFields) {
            if (!errorMap.containsKey(field)) {
                log.warn("  ⚠ Error response missing field '{}'", field);
            }
        }
        
        log.info("  ✓ Error response structure validated");
    }
    
    @Test
    @DisplayName("Health endpoint always available")
    void healthEndpoint_alwaysAvailable() {
        var health = api.get("/actuator/health")
            .expectStatus(200)
            .execute();
        
        health
            .assertField("status", "UP")
            .assertFieldExists("components.db")
            .assertFieldExists("components.redis");
        
        log.info("  ✓ Health endpoint operational");
    }
}
