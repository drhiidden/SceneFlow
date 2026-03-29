# Contributing Guide - FSJ Regressive

## Principios de Diseño

### 1. Caso de Uso sobre Endpoint

**❌ NO:**
```java
@Test
void postNews_returns201() {
    api.post("/api/news").withBody(data).expectStatus(201);
}
```

**✅ SÍ:**
```java
@Test
@DisplayName("Scenario: Admin publica noticia y aparece en home")
void adminPublishesNews_appearsOnHomepage() {
    // GIVEN: Admin autenticado
    // WHEN: Publica noticia
    // THEN: Aparece en /featured
}
```

### 2. Contexto Compartido

Usa `ScenarioContext` para pasar datos entre steps:

```java
// Step 1
Long newsId = response.extractId();
context.store("news-id", newsId);

// Step 2
Long id = context.get("news-id");
```

### 3. Cleanup Automático

**Siempre** limpia recursos en `cleanup()`:

```java
@Override
protected void cleanup() {
    if (context.has("news-id")) {
        api.delete("/api/news/" + context.get("news-id"))
            .withAuth(token)
            .execute();
    }
}
```

---

## Estructura de Test

```java
@DisplayName("Feature Name Scenarios")
class FeatureScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("Scenario: Usuario hace X → Sistema hace Y → Usuario ve Z")
    void myScenario() {
        // ============================================================
        // GIVEN: Precondiciones (setup, auth, data)
        // ============================================================
        String token = loginAsAdmin();
        api.withAuth(token);
        
        var data = Map.of("field", "value");
        
        // ============================================================
        // WHEN: Acción principal
        // ============================================================
        context.startTiming("my-action");
        var response = api.post("/api/endpoint")
            .withBody(data)
            .expectStatus(201)
            .execute();
        context.endTiming("my-action");
        
        Long id = response.extractId();
        context.store("my-id", id);
        
        // ============================================================
        // THEN: Assertions
        // ============================================================
        response
            .assertField("field", "value")
            .assertFieldNotNull("createdAt");
        
        // Verificar side effects
        api.get("/api/endpoint/" + id)
            .expectStatus(200)
            .execute()
            .assertField("status", "ACTIVE");
    }
    
    @Override
    protected void cleanup() {
        // DELETE recursos creados
    }
}
```

---

## Naming Conventions

### Classes

- `*Scenario.java`: Flujos funcionales (casos de uso)
- `*Test.java`: Tests unitarios/regresivos tradicionales
- `*Journey.java`: Flujos de usuario end-to-end

### Tests

- `@DisplayName("Scenario: [Usuario/Actor] hace X → obtiene Y")`
- Verbos: `create`, `update`, `delete`, `verify`, `appears`, `triggers`

### Context Keys

- `entity-id`: ID del recurso creado
- `entity-token`: Token/auth
- `entity-data`: Datos compartidos

---

## Assertions

### Disponibles

```java
response
    .assertStatus(200)                       // Status HTTP
    .assertField("name", "Canserbero")       // Valor exacto
    .assertFieldNotNull("id")                // No null
    .assertFieldExists("metadata")           // Existe key
    .assertArraySize("items", 5)             // Tamaño array
    .assertArrayNotEmpty("content")          // Array no vacío
    .assertContains("tags", "hip-hop");      // Array contiene valor
```

### Custom

```java
var artists = response.asList();
boolean found = artists.stream()
    .anyMatch(a -> "Canserbero".equals(a.get("name")));

if (!found) {
    throw new AssertionError("Artist not found in results");
}
```

---

## Environments

### Dev (localhost)

```bash
mvn test
```

### Docker

```bash
# Terminal 1: arrancar backend
cd ../FSG-WIKIRAP/back
docker-compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# Terminal 2: tests
cd FSJ-Regressive
mvn test -Dtest.env=docker
```

### Staging/Prod (read-only)

```bash
export API_BASE_URL=https://staging.wikirap.com
export ADMIN_USERNAME=test-user
export ADMIN_PASSWORD=secret

mvn test -Dtest.env=prod
```

⚠️ **IMPORTANTE**: En prod, `cleanup.after.test=false` para NO borrar datos.

---

## Debugging

### Ver response completo

```java
response.printResponse();
```

### Ver contexto

```java
context.printContext();
```

### Logs más verbosos

```bash
mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

### Ejecutar un solo test

```bash
mvn test -Dtest=CMSWorkflowScenario#publishNews_appearsInListings
```

---

## Performance Testing

```java
context.startTiming("import-batch");
api.post("/api/artists/import/batch")...
long duration = context.endTiming("import-batch");

if (duration > 5000) {
    throw new AssertionError("Import took " + duration + "ms (max: 5000ms)");
}
```

---

## CI/CD Integration

Ver `Readme.md` sección "Integración CI/CD" para ejemplo completo de GitHub Actions.

**Key points:**
- MySQL + Redis como services
- `sleep 30` para esperar backend startup
- Upload test reports como artifacts
- Fallar pipeline si tests fallan

---

## Preguntas Frecuentes

### ¿Cómo añadir auth JWT?

```java
String token = loginAsAdmin();
api.withAuth(token);  // Aplica a todos los requests
```

### ¿Cómo testear endpoints protegidos?

```java
api.get("/api/admin/users")
    .withAuth(adminToken)
    .expectStatus(200)
    .execute();
```

### ¿Cómo verificar paginación?

```java
api.get("/api/artists")
    .withQuery("page", 0)
    .withQuery("size", 10)
    .expectStatus(200)
    .execute()
    .assertField("pageable.pageSize", 10)
    .assertField("number", 0);
```

### ¿Cómo testear errores 4xx?

```java
api.post("/api/news")
    .withBody(Map.of("title", "No category"))
    .expectStatus(400)
    .execute()
    .assertFieldExists("message");
```

---

**Happy Testing!** 🚀
