package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Example CMS workflow scenarios without authentication.
 *
 * Demonstrates SceneFlow for testing create → verify → cleanup flows
 * on endpoints configured with permitAll() security.
 *
 * Adapt entity fields, categories and endpoint paths to match your API.
 */
@Slf4j
@DisplayName("CMS Workflow Scenarios (No Auth)")
class CMSWorkflowNoAuthScenario extends ScenarioRunner {

    @Test
    @DisplayName("Scenario: Create article and verify it appears in listings")
    void publishArticle_appearsInListings_noAuth() {
        var articleData = Map.of(
            "title", "SceneFlow Demo Article - E2E Test",
            "content", "This article was created by SceneFlow during an automated test run.",
            "excerpt", "SceneFlow end-to-end test article",
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
            .assertFieldNotNull("title")
            .assertField("category", "TECHNOLOGY")
            .assertField("featured", false);

        log.info("✓ Article created with ID: {}", articleId);

        waitForDbCommit();

        // Verify it appears in the general listing
        var articleList = api.get("/api/articles")
            .expectStatus(200)
            .execute();

        articleList
            .assertFieldExists("content")
            .assertArrayNotEmpty("content");

        log.info("✓ Article appears in general listing");

        // Verify it appears in the category listing
        var categoryArticles = api.get("/api/articles/category/TECHNOLOGY")
            .expectStatus(200)
            .execute()
            .asList();

        boolean found = categoryArticles.stream()
            .anyMatch(a -> articleId.equals(((Number) a.get("id")).longValue()));

        if (found) {
            log.info("✓ Article appears in TECHNOLOGY category");
        } else {
            throw new AssertionError("Article not found in TECHNOLOGY category");
        }
    }

    @Test
    @DisplayName("Scenario: Toggle featured flag on article")
    void featureArticle_togglesCorrectly() {
        var articleId = api.post("/api/articles")
            .withBody(Map.of(
                "title", "Test Featured Toggle",
                "content", "Content",
                "category", "GENERAL",
                "date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "featured", false
            ))
            .expectStatus(201)
            .execute()
            .extractId();

        context.store("article-id", articleId);

        waitForDbCommit();

        api.patch("/api/articles/" + articleId + "/toggle-featured")
            .expectStatus(200)
            .execute()
            .assertField("featured", true);

        log.info("✓ Article marked as featured");

        var featured = api.get("/api/articles/featured")
            .expectStatus(200)
            .execute()
            .asList();

        boolean inFeatured = featured.stream()
            .anyMatch(a -> articleId.equals(((Number) a.get("id")).longValue()));

        if (inFeatured) {
            log.info("✓ Article appears in /featured");
        }
    }

    @Test
    @DisplayName("Scenario: Create hero carousel item and verify in active list")
    void createHeroCarousel_appearsInActive() {
        var carouselData = Map.of(
            "mediaUrl", "https://example.com/test-carousel.jpg",
            "mediaType", "IMAGE",
            "altText", "SceneFlow E2E Test Banner",
            "displayOrder", 99,
            "isActive", true,
            "caption", "Automated test carousel item"
        );

        var createResponse = api.post("/api/cms/hero-carousel")
            .withBody(carouselData)
            .expectStatus(201)
            .execute();

        Long carouselId = createResponse.extractId();
        context.store("carousel-id", carouselId);

        createResponse
            .assertField("mediaType", "IMAGE")
            .assertField("displayOrder", 99)
            .assertField("isActive", true);

        log.info("✓ Carousel item created with ID: {}", carouselId);

        waitForDbCommit();

        var activeItems = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();

        boolean found = activeItems.stream()
            .anyMatch(c -> carouselId.equals(((Number) c.get("id")).longValue()));

        if (found) {
            log.info("✓ Carousel item appears in /active");
        } else {
            throw new AssertionError("Carousel item not found in /active");
        }
    }

    @Override
    protected void cleanup() {
        if (context.has("article-id")) {
            Long articleId = context.get("article-id");
            try {
                api.delete("/api/articles/" + articleId).execute();
                log.info("  [Cleanup] Deleted article #{}", articleId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Could not delete article #{}: {}", articleId, e.getMessage());
            }
        }

        if (context.has("carousel-id")) {
            Long carouselId = context.get("carousel-id");
            try {
                api.delete("/api/cms/hero-carousel/" + carouselId).execute();
                log.info("  [Cleanup] Deleted carousel #{}", carouselId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Could not delete carousel #{}: {}", carouselId, e.getMessage());
            }
        }
    }
}
