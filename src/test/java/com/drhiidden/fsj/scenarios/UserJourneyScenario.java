package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Escenarios desde perspectiva del usuario final.
 * 
 * Journey completo:
 * 1. Usuario busca artista
 * 2. Ve perfil + discografía
 * 3. Reproduce canción
 * 4. Comenta
 */
@Slf4j
@DisplayName("User Journey Scenarios")
class UserJourneyScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("Journey: Descubrir artista → Ver perfil → Explorar discografía")
    void discoverArtist_exploreProfile_browseDiscography() {
        // GIVEN: Usuario en home
        log.info("  [User] Usuario accede a WikiRAP");
        
        // WHEN: Ve featured news
        var featuredNews = api.get("/api/news/featured")
            .expectStatus(200)
            .execute()
            .asList();
        
        if (!featuredNews.isEmpty()) {
            log.info("  [User] ✓ Ve {} noticias destacadas", featuredNews.size());
        }
        
        // AND: Ve hero carousel
        var carousel = api.get("/api/cms/hero-carousel/active")
            .expectStatus(200)
            .execute()
            .asList();
        
        if (!carousel.isEmpty()) {
            log.info("  [User] ✓ Ve carousel con {} items", carousel.size());
        }
        
        // WHEN: Busca artista
        var searchResults = api.get("/api/artists/search")
            .withQuery("q", "canserbero")
            .expectStatus(200)
            .execute()
            .asList();
        
        log.info("  [User] Busca 'canserbero' → {} resultados", searchResults.size());
        
        // THEN: Si hay resultados, explora perfil
        if (!searchResults.isEmpty()) {
            Long artistId = ((Number) searchResults.get(0).get("id")).longValue();
            
            var profile = api.get("/api/artists/" + artistId)
                .expectStatus(200)
                .execute();
            
            profile
                .assertFieldNotNull("name")
                .assertFieldExists("biography");
            
            String artistName = profile.extract("name");
            log.info("  [User] ✓ Perfil cargado: {}", artistName);
            
            // Ver álbumes del artista
            api.get("/api/albums")
                .withQuery("artistId", artistId)
                .expectStatus(200)
                .execute()
                .assertFieldExists("content");
            
            log.info("  [User] ✓ Discografía explorada");
        }
    }
    
    @Test
    @DisplayName("Journey: Ver trending → Explorar recomendaciones")
    void exploreTrending_getRecommendations() {
        // WHEN: Ve artistas trending
        var trending = api.get("/api/metrics/trending")
            .expectStatus(200)
            .execute();
        
        trending.assertFieldExists("$");
        
        var trendingList = trending.asList();
        log.info("  [User] ✓ Ve {} artistas trending", trendingList.size());
        
        // AND: Solicita recomendaciones
        api.get("/api/recommendations")
            .expectStatus(200)
            .execute()
            .assertFieldExists("$");
        
        log.info("  [User] ✓ Recomendaciones obtenidas");
    }
    
    @Test
    @DisplayName("Journey: Explorar batallas → Ver detalles → Comentarios")
    void exploreBattles_viewDetails_readComments() {
        // WHEN: Lista batallas
        var battles = api.get("/api/battles")
            .expectStatus(200)
            .execute();
        
        battles.assertFieldExists("content");
        
        var battlesList = battles.extract("content");
        log.info("  [User] ✓ Ve listado de batallas");
        
        // Si hay batallas, ver detalles
        if (battlesList instanceof List && !((List<?>) battlesList).isEmpty()) {
            Map<String, Object> firstBattle = (Map<String, Object>) ((List<?>) battlesList).get(0);
            Long battleId = ((Number) firstBattle.get("id")).longValue();
            
            api.get("/api/battles/" + battleId)
                .expectStatus(200)
                .execute()
                .assertFieldNotNull("name")
                .assertFieldExists("format");
            
            log.info("  [User] ✓ Detalles de batalla cargados");
            
            // Ver comentarios
            api.get("/api/battles/" + battleId + "/comments")
                .expectStatus(200)
                .execute()
                .assertFieldExists("$");
            
            log.info("  [User] ✓ Comentarios visibles");
        }
    }
}
