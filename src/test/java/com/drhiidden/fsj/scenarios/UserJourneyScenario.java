package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Example user journey scenarios from the end-user perspective.
 *
 * Demonstrates SceneFlow for testing complete user flows:
 * 1. User searches for content → views detail → explores related items
 * 2. User browses trending → gets recommendations
 * 3. User views featured content → reads details
 *
 * Adapt entity types and endpoint paths to match your API.
 */
@Slf4j
@DisplayName("User Journey Scenarios")
class UserJourneyScenario extends ScenarioRunner {

    @Test
    @DisplayName("Journey: Search product → View profile → Explore related items")
    void searchProduct_viewProfile_exploreRelated() {
        log.info("  [User] Accesses the home page");

        // User sees featured articles on the home page
        var featuredArticles = api.get("/api/articles/featured")
            .expectStatus(200)
            .execute()
            .asList();

        if (!featuredArticles.isEmpty()) {
            log.info("  [User] ✓ Sees {} featured articles", featuredArticles.size());
        }

        // User sees the hero carousel
        var carousel = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();

        if (!carousel.isEmpty()) {
            log.info("  [User] ✓ Sees carousel with {} items", carousel.size());
        }

        // User searches for a product/resource
        var searchResults = api.get("/api/products/search")
            .withQuery("q", "test-product")
            .expectStatus(200)
            .execute()
            .asList();

        log.info("  [User] Searches 'test-product' → {} results", searchResults.size());

        // If results exist, explore the first product profile
        if (!searchResults.isEmpty()) {
            Long productId = ((Number) searchResults.get(0).get("id")).longValue();

            var profile = api.get("/api/products/" + productId)
                .expectStatus(200)
                .execute();

            profile
                .assertFieldNotNull("name")
                .assertFieldExists("description");

            String productName = profile.extract("name");
            log.info("  [User] ✓ Profile loaded: {}", productName);

            // Explore related items (e.g. collections, variants)
            api.get("/api/collections")
                .withQuery("productId", productId)
                .expectStatus(200)
                .execute()
                .assertFieldExists("content");

            log.info("  [User] ✓ Related items explored");
        }
    }

    @Test
    @DisplayName("Journey: Browse trending → Get recommendations")
    void exploreTrending_getRecommendations() {
        var trending = api.get("/api/metrics/trending")
            .expectStatus(200)
            .execute();

        trending.assertFieldExists("$");

        var trendingList = trending.asList();
        log.info("  [User] ✓ Sees {} trending items", trendingList.size());

        api.get("/api/recommendations")
            .expectStatus(200)
            .execute()
            .assertFieldExists("$");

        log.info("  [User] ✓ Recommendations loaded");
    }

    @Test
    @DisplayName("Journey: Browse events → View details → Read comments")
    void browseEvents_viewDetails_readComments() {
        var events = api.get("/api/events")
            .expectStatus(200)
            .execute();

        events.assertFieldExists("content");

        var eventsList = events.extract("content");
        log.info("  [User] ✓ Sees event listing");

        if (eventsList instanceof List && !((List<?>) eventsList).isEmpty()) {
            Map<String, Object> firstEvent = (Map<String, Object>) ((List<?>) eventsList).get(0);
            Long eventId = ((Number) firstEvent.get("id")).longValue();

            api.get("/api/events/" + eventId)
                .expectStatus(200)
                .execute()
                .assertFieldNotNull("name")
                .assertFieldExists("description");

            log.info("  [User] ✓ Event details loaded");

            api.get("/api/events/" + eventId + "/comments")
                .expectStatus(200)
                .execute()
                .assertFieldExists("$");

            log.info("  [User] ✓ Comments visible");
        }
    }
}
