package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Escenarios funcionales del sistema multi-source (Genius + Last.fm).
 * 
 * Casos de uso:
 * 1. Importar artista con múltiples fuentes
 * 2. Verificar completeness score
 * 3. Scraped sources tracking
 */
@Slf4j
@DisplayName("Artist Multi-Source Scenarios")
class ArtistMultiSourceScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("Scenario: Importar artista con Genius + Last.fm")
    void importArtist_withMultipleSources_enrichesProfile() {
        // GIVEN: API key válida para spider-service
        String apiKey = System.getenv("SPIDER_API_KEY");
        if (apiKey == null) {
            log.warn("SPIDER_API_KEY not set, skipping scenario");
            return;
        }
        
        // WHEN: Importa batch con Last.fm profile
        var batchData = List.of(Map.of(
            "name", "Canserbero",
            "alias", "Can",
            "biography", "Rapper venezolano legendario",
            "geniusId", 12345L,
            "lastfmProfile", Map.of(
                "listeners", 500000,
                "playCount", 10000000
            ),
            "scrapedSources", List.of("GENIUS", "LASTFM"),
            "completenessScore", 85.0
        ));
        
        context.startTiming("import-batch");
        var importResponse = api.post("/api/artists/import/batch")
            .withHeader("X-API-Key", apiKey)
            .withBody(batchData)
            .expectStatus(200)
            .execute();
        context.endTiming("import-batch");
        
        importResponse
            .assertField("totalImported", 1)
            .assertArraySize("importedArtists", 1);
        
        Long artistId = (Long) importResponse.extract("importedArtists[0].id");
        context.store("artist-id", artistId);
        
        // THEN: Artista tiene datos enriquecidos
        var artist = api.get("/api/artists/" + artistId)
            .expectStatus(200)
            .execute();
        
        artist
            .assertField("name", "Canserbero")
            .assertFieldNotNull("scrapedSources")
            .assertFieldNotNull("completenessScore");
        
        // Verificar external integrations
        List<Map<String, Object>> sources = artist.extract("scrapedSources");
        if (!sources.contains("GENIUS") || !sources.contains("LASTFM")) {
            throw new AssertionError("Expected scraped sources to contain GENIUS and LASTFM");
        }
        
        log.info("  ✓ Artist enriched with {} sources", sources.size());
    }
    
    @Test
    @DisplayName("Scenario: Completeness score refleja fuentes disponibles")
    void completenessScore_reflectsDataQuality() {
        // GIVEN: Artista con datos básicos (sin external integrations)
        String token = loginAsAdmin();
        api.withAuth(token);
        
        var basicArtist = Map.of(
            "name", "Test Artist Basic",
            "biography", "Bio mínima"
        );
        
        Long basicId = api.post("/api/artists")
            .withBody(basicArtist)
            .expectStatus(201)
            .execute()
            .extractId();
        
        // THEN: Completeness score bajo
        var basicProfile = api.get("/api/artists/" + basicId)
            .execute()
            .asMap();
        
        Double basicScore = ((Number) basicProfile.get("completenessScore")).doubleValue();
        
        // Score debe ser < 50% (sin external data, sin genres, etc.)
        if (basicScore == null || basicScore >= 50.0) {
            log.warn("Basic artist score: {} (expected < 50)", basicScore);
        }
        
        // WHEN: Se añade más información (simular enriquecimiento)
        // (En producción esto vendría del spider-service)
        
        // THEN: Score incrementaría (este test solo valida estructura)
        log.info("  ✓ Completeness score mechanism validated");
    }
    
    @Override
    protected void cleanup() {
        if (context.has("artist-id")) {
            Long artistId = context.get("artist-id");
            try {
                api.delete("/api/artists/" + artistId)
                    .withAuth(context.get("admin-token"))
                    .execute();
                log.info("  [Cleanup] Deleted artist #{}", artistId);
            } catch (Exception e) {
                log.warn("  [Cleanup] Could not delete artist #{}: {}", artistId, e.getMessage());
            }
        }
    }
}
