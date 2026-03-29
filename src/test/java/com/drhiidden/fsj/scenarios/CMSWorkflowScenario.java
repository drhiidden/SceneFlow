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
 * 
 * Casos de uso:
 * 1. Admin publica noticia → aparece en listado
 * 2. Admin destaca noticia → aparece en /featured
 * 3. Admin crea hero carousel → aparece en /active
 * 4. Reordenar carousel items
 */
@Slf4j
@DisplayName("CMS Workflow Scenarios")
class CMSWorkflowScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("Scenario: Publicar noticia y verificar en listados")
    void publishNews_appearsInListings() {
        // GIVEN: Admin autenticado
        String token = loginAsAdmin();
        api.withAuth(token);
        
        // WHEN: Crea noticia de batalla
        var newsData = Map.of(
            "title", "FMS Internacional 2026 - Semifinales",
            "content", "Análisis completo de las semifinales de FMS con estadísticas",
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
            .assertField("title", "FMS Internacional 2026 - Semifinales")
            .assertField("category", "BATALLAS")
            .assertField("featured", false);
        
        // THEN: Aparece en listado general
        api.get("/api/news")
            .expectStatus(200)
            .execute()
            .assertFieldExists("content")
            .assertArrayNotEmpty("content");
        
        // AND: Aparece en listado por categoría
        api.get("/api/news/category/" + "BATALLAS")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$");
    }
    
    @Test
    @DisplayName("Scenario: Destacar noticia y verificar en featured")
    void featureNews_appearsInFeatured() {
        // GIVEN: Noticia existente no destacada
        String token = loginAsAdmin();
        api.withAuth(token);
        
        var newsId = api.post("/api/news")
            .withBody(Map.of(
                "title", "Test Featured News",
                "content", "Content",
                "category", "GENERAL",
                "date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "featured", false
            ))
            .expectStatus(201)
            .execute()
            .extractId();
        
        context.store("news-id", newsId);
        
        // WHEN: Se marca como destacada
        api.patch("/api/news/" + newsId + "/toggle-featured")
            .expectStatus(200)
            .execute()
            .assertField("featured", true);
        
        // THEN: Aparece en endpoint featured
        api.get("/api/news/featured")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$")
            .assertFieldNotNull("[0].id");
        
        // Verificar que está en el array
        var featured = api.get("/api/news/featured")
            .execute()
            .asList();
        
        boolean found = featured.stream()
            .anyMatch(n -> newsId.equals(((Number) n.get("id")).longValue()));
        
        if (!found) {
            throw new AssertionError("Featured news " + newsId + " not found in /featured endpoint");
        }
    }
    
    @Test
    @DisplayName("Scenario: Crear hero carousel y verificar en active")
    void createHeroCarousel_appearsInActive() {
        // GIVEN: Admin autenticado
        String token = loginAsAdmin();
        api.withAuth(token);
        
        // WHEN: Crea item de carousel
        var carouselData = Map.of(
            "mediaUrl", "https://example.com/fms-2026.jpg",
            "mediaType", "IMAGE",
            "altText", "FMS Internacional 2026",
            "displayOrder", 1,
            "isActive", true,
            "caption", "Las mejores batallas del año"
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
        
        // THEN: Aparece en endpoint active
        api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .assertArrayNotEmpty("$")
            .assertFieldNotNull("[0].mediaUrl");
    }
    
    @Test
    @DisplayName("Scenario: Reordenar carousel items")
    void reorderCarousel_updatesDisplayOrder() {
        // GIVEN: 3 items de carousel
        String token = loginAsAdmin();
        api.withAuth(token);
        
        Long id1 = createCarouselItem(1, "Item 1");
        Long id2 = createCarouselItem(2, "Item 2");
        Long id3 = createCarouselItem(3, "Item 3");
        
        // WHEN: Reordena (3 → 1, 1 → 2, 2 → 3)
        api.put("/api/cms/hero-carousel/" + id3 + "/reorder")
            .withQuery("newOrder", 1)
            .expectStatus(200)
            .execute();
        
        // THEN: El orden cambió
        var activeItems = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();
        
        // Verificar orden: id3 debe estar primero
        Long firstId = ((Number) activeItems.get(0).get("id")).longValue();
        if (!firstId.equals(id3)) {
            throw new AssertionError("Expected first item to be " + id3 + " but was " + firstId);
        }
    }
    
    @Override
    protected void cleanup() {
        // Limpiar recursos creados
        if (context.has("news-id") && config.isCleanupAfterTest()) {
            Long newsId = context.get("news-id");
            try {
                api.delete("/api/news/" + newsId)
                    .withAuth(context.get("admin-token"))
                    .execute();
                log.info("  [Cleanup] Deleted news #{}", newsId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Failed to delete news #{}: {}", newsId, e.getMessage());
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
    
    /**
     * Helper: crear item de carousel.
     */
    private Long createCarouselItem(int order, String altText) {
        return api.post("/api/cms/hero-carousel")
            .withBody(Map.of(
                "mediaUrl", "https://example.com/item-" + order + ".jpg",
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
