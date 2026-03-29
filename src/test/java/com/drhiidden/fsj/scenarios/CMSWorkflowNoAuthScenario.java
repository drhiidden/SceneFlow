package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Escenarios funcionales del flujo CMS (News + Hero Carousel).
 * SIN AUTH: Usa endpoints permitAll() configurados en SecurityConfig.
 */
@Slf4j
@DisplayName("CMS Workflow Scenarios (No Auth)")
class CMSWorkflowNoAuthScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("Scenario: Crear noticia y verificar en listados (NO AUTH)")
    void publishNews_appearsInListings_noAuth() {
        // WHEN: Crea noticia (endpoint permitAll)
        var newsData = Map.of(
            "title", "FMS Internacional 2026 - Semifinales E2E Test",
            "content", "Análisis completo de las semifinales con estadísticas",
            "excerpt", "Las semifinales más reñidas de la historia",
            "category", "BATALLAS",
            "date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "featured", false
        );
        
        context.startTiming("create-news");
        var createResponse = api.post("/api/news")
            .withBody(newsData)
            .expectStatus(201)
            .execute();
        context.endTiming("create-news");
        
        Long newsId = createResponse.extractId();
        context.store("news-id", newsId);
        
        createResponse
            .assertFieldNotNull("title")
            .assertField("category", "BATALLAS")
            .assertField("featured", false);
        
        log.info("✓ Noticia creada con ID: {}", newsId);
        
        // Wait for DB commit
        waitForDbCommit();
        
        // THEN: Aparece en listado general
        var newsList = api.get("/api/news")
            .expectStatus(200)
            .execute();
        
        newsList
            .assertFieldExists("content")
            .assertArrayNotEmpty("content");
        
        log.info("✓ Noticia aparece en listado general");
        
        // AND: Aparece en listado por categoría
        var categoryNews = api.get("/api/news/category/BATALLAS")
            .expectStatus(200)
            .execute()
            .asList();
        
        boolean found = categoryNews.stream()
            .anyMatch(n -> newsId.equals(((Number) n.get("id")).longValue()));
        
        if (found) {
            log.info("✓ Noticia aparece en categoría BATALLAS");
        } else {
            throw new AssertionError("Noticia no encontrada en categoría BATALLAS");
        }
    }
    
    @Test
    @DisplayName("Scenario: Toggle featured news (NO AUTH)")
    void featureNews_togglesCorrectly() {
        // GIVEN: Noticia existente
        var newsId = api.post("/api/news")
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
        
        context.store("news-id", newsId);
        
        // Wait for DB commit
        waitForDbCommit();
        
        // WHEN: Toggle featured
        api.patch("/api/news/" + newsId + "/toggle-featured")
            .expectStatus(200)
            .execute()
            .assertField("featured", true);
        
        log.info("✓ Noticia marcada como destacada");
        
        // THEN: Aparece en /featured
        var featured = api.get("/api/news/featured")
            .expectStatus(200)
            .execute()
            .asList();
        
        boolean inFeatured = featured.stream()
            .anyMatch(n -> newsId.equals(((Number) n.get("id")).longValue()));
        
        if (inFeatured) {
            log.info("✓ Noticia aparece en /featured");
        }
    }
    
    @Test
    @DisplayName("Scenario: Crear y activar hero carousel (NO AUTH)")
    void createHeroCarousel_appearsInActive() {
        // WHEN: Crea carousel item
        var carouselData = Map.of(
            "mediaUrl", "https://example.com/test-carousel.jpg",
            "mediaType", "IMAGE",
            "altText", "Test Carousel E2E",
            "displayOrder", 99,
            "isActive", true,
            "caption", "Testing carousel"
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
        
        log.info("✓ Carousel item creado con ID: {}", carouselId);
        
        // Wait for DB commit
        waitForDbCommit();
        
        // THEN: Aparece en /active
        var activeItems = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();
        
        boolean found = activeItems.stream()
            .anyMatch(c -> carouselId.equals(((Number) c.get("id")).longValue()));
        
        if (found) {
            log.info("✓ Carousel item aparece en /active");
        } else {
            throw new AssertionError("Carousel item no encontrado en /active");
        }
    }
    
    @Override
    protected void cleanup() {
        // Cleanup sin auth (endpoints permitAll)
        if (context.has("news-id")) {
            Long newsId = context.get("news-id");
            try {
                api.delete("/api/news/" + newsId).execute();
                log.info("  [Cleanup] Deleted news #{}", newsId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Could not delete news #{}: {}", newsId, e.getMessage());
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
