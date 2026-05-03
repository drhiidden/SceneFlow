package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Example CMS workflow scenarios with admin authentication.
 *
 * Demonstrates SceneFlow for testing authenticated admin flows:
 * 1. Admin publishes article → appears in listings
 * 2. Admin features article → appears in /featured
 * 3. Admin creates hero carousel → appears in /active
 * 4. Admin reorders carousel items
 *
 * Adapt entity fields, categories and endpoint paths to match your API.
 */
@Slf4j
@DisplayName("CMS Workflow Scenarios")
class CMSWorkflowScenario extends ScenarioRunner {

    @Test
    @DisplayName("Scenario: Publish article and verify in listings")
    void publishArticle_appearsInListings() {
        String token = loginAsAdmin();
        api.withAuth(token);

        var articleData = Map.of(
            "title", "SceneFlow Framework - Getting Started",
            "content", "A practical guide to scenario-based API testing with SceneFlow.",
            "excerpt", "Test user journeys, not just endpoints.",
            "category", "TECHNOLOGY",
            "date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "featured", false
        );

        context.startTiming("create-article");
        var createResponse = api.post("/api/articles")
            .withBody(articleData)
            .expectStatus(201)
            .execute();
        context.endTiming("create-article");

        Long articleId = createResponse.extractId();
        context.store("article-id", articleId);

        createResponse
            .assertField("title", "SceneFlow Framework - Getting Started")
            .assertField("category", "TECHNOLOGY")
            .assertField("featured", false);

        api.get("/api/articles")
            .expectStatus(200)
            .execute()
            .assertFieldExists("content")
            .assertArrayNotEmpty("content");

        api.get("/api/articles/category/TECHNOLOGY")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$");
    }

    @Test
    @DisplayName("Scenario: Feature article and verify in /featured endpoint")
    void featureArticle_appearsInFeatured() {
        String token = loginAsAdmin();
        api.withAuth(token);

        var articleId = api.post("/api/articles")
            .withBody(Map.of(
                "title", "Test Featured Article",
                "content", "Content",
                "category", "GENERAL",
                "date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "featured", false
            ))
            .expectStatus(201)
            .execute()
            .extractId();

        context.store("article-id", articleId);

        api.patch("/api/articles/" + articleId + "/toggle-featured")
            .expectStatus(200)
            .execute()
            .assertField("featured", true);

        api.get("/api/articles/featured")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$")
            .assertFieldNotNull("[0].id");

        var featured = api.get("/api/articles/featured").execute().asList();

        boolean found = featured.stream()
            .anyMatch(a -> articleId.equals(((Number) a.get("id")).longValue()));

        if (!found) {
            throw new AssertionError("Featured article " + articleId + " not found in /featured endpoint");
        }
    }

    @Test
    @DisplayName("Scenario: Create hero carousel item and verify in /active")
    void createHeroCarousel_appearsInActive() {
        String token = loginAsAdmin();
        api.withAuth(token);

        var carouselData = Map.of(
            "mediaUrl", "https://example.com/banner-2026.jpg",
            "mediaType", "IMAGE",
            "altText", "SceneFlow 2026 Banner",
            "displayOrder", 1,
            "isActive", true,
            "caption", "Automated test banner"
        );

        var createResponse = api.post("/api/cms/hero-carousel")
            .withBody(carouselData)
            .expectStatus(201)
            .execute();

        Long carouselId = createResponse.extractId();
        context.store("carousel-id", carouselId);

        createResponse
            .assertField("mediaType", "IMAGE")
            .assertField("displayOrder", 1)
            .assertField("isActive", true);

        api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$")
            .assertFieldNotNull("[0].mediaUrl");
    }

    @Test
    @DisplayName("Scenario: Reorder carousel items updates displayOrder")
    void reorderCarousel_updatesDisplayOrder() {
        String token = loginAsAdmin();
        api.withAuth(token);

        Long id1 = createCarouselItem(1, "Banner A");
        Long id2 = createCarouselItem(2, "Banner B");
        Long id3 = createCarouselItem(3, "Banner C");

        api.put("/api/cms/hero-carousel/" + id3 + "/reorder")
            .withQuery("newOrder", 1)
            .expectStatus(200)
            .execute();

        var activeItems = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();

        Long firstId = ((Number) activeItems.get(0).get("id")).longValue();
        if (!firstId.equals(id3)) {
            throw new AssertionError("Expected first item to be " + id3 + " but was " + firstId);
        }
    }

    @Override
    protected void cleanup() {
        if (context.has("article-id") && config.isCleanupAfterTest()) {
            Long articleId = context.get("article-id");
            try {
                api.delete("/api/articles/" + articleId)
                    .withAuth(context.get("admin-token"))
                    .execute();
                log.info("  [Cleanup] Deleted article #{}", articleId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Failed to delete article #{}: {}", articleId, e.getMessage());
            }
        }

        if (context.has("carousel-id") && config.isCleanupAfterTest()) {
            Long carouselId = context.get("carousel-id");
            try {
                api.delete("/api/cms/hero-carousel/" + carouselId)
                    .withAuth(context.get("admin-token"))
                    .execute();
                log.info("  [Cleanup] Deleted carousel #{}", carouselId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Failed to delete carousel #{}: {}", carouselId, e.getMessage());
            }
        }
    }

    private Long createCarouselItem(int order, String altText) {
        return api.post("/api/cms/hero-carousel")
            .withBody(Map.of(
                "mediaUrl", "https://example.com/banner-" + order + ".jpg",
                "mediaType", "IMAGE",
                "altText", altText,
                "displayOrder", order,
                "isActive", true
            ))
            .expectStatus(201)
            .execute()
            .extractId();
    }
}
