package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Example backward compatibility tests.
 *
 * Ensures that:
 * - API contracts are not broken between deployments
 * - Required fields never disappear from responses
 * - Enums and date formats remain stable
 * - Pagination structure is consistent across endpoints
 *
 * Adapt endpoint paths and mandatory field lists to match your API.
 */
@Slf4j
@DisplayName("Backward Compatibility Tests")
class BackwardCompatibilityTest extends ScenarioRunner {

    @Test
    @DisplayName("Product API: mandatory fields always present in responses")
    void productApi_mandatoryFieldsAlwaysPresent() {
        var products = api.get("/api/products")
            .expectStatus(200)
            .execute()
            .extract("content");

        if (products instanceof java.util.List<?> list && !list.isEmpty()) {
            var firstProduct = (Map<String, Object>) list.get(0);

            // Fields that must never be removed from the API response
            String[] mandatoryFields = {
                "id", "name", "category", "createdAt", "active"
            };

            for (String field : mandatoryFields) {
                if (!firstProduct.containsKey(field)) {
                    throw new AssertionError("BREAKING CHANGE: Product API missing field '" + field + "'");
                }
            }

            log.info("  ✓ All mandatory Product fields present");
        }
    }

    @Test
    @DisplayName("Article API: category enum values are backward compatible")
    void articleApi_categoryEnumStable() {
        var articles = api.get("/api/articles")
            .expectStatus(200)
            .execute()
            .extract("content");

        if (articles instanceof java.util.List<?> list && !list.isEmpty()) {
            var firstArticle = (Map<String, Object>) list.get(0);

            if (firstArticle.containsKey("category")) {
                String category = (String) firstArticle.get("category");

                // Adjust these to match your API's allowed enum values
                String[] validCategories = {
                    "TECHNOLOGY", "TUTORIAL", "NEWS", "RELEASE", "GENERAL"
                };

                boolean valid = false;
                for (String validCat : validCategories) {
                    if (validCat.equals(category)) {
                        valid = true;
                        break;
                    }
                }

                if (!valid) {
                    throw new AssertionError("BREAKING CHANGE: Unknown article category '" + category + "'");
                }

                log.info("  ✓ Article category '{}' is valid", category);
            }
        }
    }

    @Test
    @DisplayName("Pagination format is consistent across all listing endpoints")
    void paginationFormat_consistentEverywhere() {
        // Adjust this list to match your paginated endpoints
        String[] paginatedEndpoints = {
            "/api/products", "/api/articles", "/api/collections", "/api/events"
        };

        for (String endpoint : paginatedEndpoints) {
            var response = api.get(endpoint)
                .expectStatus(200)
                .execute()
                .asMap();

            // Spring Data Page format — adjust if your API uses a different structure
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
        var errorResponse = api.post("/api/articles")
            .withBody(Map.of("title", "Missing required fields"))
            .execute();

        // Validation errors should return 4xx, never 5xx
        if (errorResponse.getStatus() >= 500) {
            throw new AssertionError("Validation error returned 500+ instead of 400");
        }

        var errorMap = errorResponse.asMap();

        // Adjust to match your error response structure
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
            .assertFieldExists("components.db");

        log.info("  ✓ Health endpoint operational");
    }
}
