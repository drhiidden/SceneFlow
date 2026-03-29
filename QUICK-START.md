# SceneFlow - Quick Start

**Test user journeys, not just endpoints**

---

## 5-Minute Setup

```bash
# 1. Navigate to framework
cd FSJ-Regressive

# 2. Ensure backend is running
curl http://localhost:8082/actuator/health

# 3. Run stable tests
mvn test -Dtest=BackwardCompatibilityTest

# 4. Run complete suite
mvn test
```

**That's it!** Framework is pre-configured for WikiRAP API.

---

## Expected Results

### BackwardCompatibilityTest (5/5 ✅)

```bash
$ mvn test -Dtest=BackwardCompatibilityTest

[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (0.2s)

✓ Artist API: required fields present
✓ News API: category enum stable
✓ Error responses: consistent structure
✓ Pagination: format unchanged
✓ Health endpoint: always available
```

**Purpose:** Validate API contracts don't break between versions.

### RegressionSmokeTest (4/4 ✅)

```bash
$ mvn test -Dtest=RegressionSmokeTest

[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (1.5s)

✓ Public endpoints: no crashes (500s)
✓ Core endpoints: expected structure
✓ Search: works across entities
✓ Pagination: consistent everywhere
```

**Purpose:** Smoke test all critical public endpoints.

### CMSWorkflowNoAuthScenario (3/3 ✅)

```bash
$ mvn test -Dtest=CMSWorkflowNoAuthScenario

[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (0.9s)

[main] INFO ▶ POST /api/news (expected: 201)
[main] INFO ✓ /api/news (status: 201) [42ms]
[main] INFO   ✓ Created news with ID: 42

[main] INFO ▶ PATCH /api/news/42/toggle-featured (expected: 200)
[main] INFO ✓ /api/news/42/toggle-featured (status: 200) [18ms]
[main] INFO   ✓ News featured successfully

[main] INFO ▶ GET /api/news/featured (expected: 200)
[main] INFO ✓ /api/news/featured (status: 200) [8ms]
[main] INFO   ✓ News appears in featured section

[main] INFO ▶ DELETE /api/news/42 (expected: any)
[main] INFO ✓ /api/news/42 (status: 204) [5ms]
[main] INFO   [Cleanup] Deleted news #42

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ SCENARIO COMPLETED (300ms)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Purpose:** Validate complete CMS workflows without authentication.

---

## Complete Suite (12 tests, 8s)

```bash
$ mvn test

Results:

RegressionSmokeTest           4/4 ✅ (1.5s)
BackwardCompatibilityTest     5/5 ✅ (0.2s)
CMSWorkflowNoAuthScenario     3/3 ✅ (0.9s)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 8.155s
BUILD SUCCESS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Multi-Environment Testing

### Dev (localhost)

```bash
mvn test
# Uses test-dev.properties automatically
```

### Docker

```bash
mvn test -Dtest.env=docker
# Uses test-docker.properties
# Expects backend at http://backend:8082
```

### Staging/Production

```bash
mvn test -Dtest.env=prod \
         -DAPI_BASE_URL=https://staging.api.com \
         -Dadmin.username=stg_admin
```

---

## Configuration Files

```
src/test/resources/
├── test-dev.properties       # Localhost (default)
├── test-docker.properties    # Docker Compose
└── test-prod.properties      # Staging/Prod
```

**Example (`test-dev.properties`):**

```properties
api.base.url=http://localhost:8082
api.timeout=30000
test.cleanup=true
admin.username=admin@test.com
admin.password=admin123
```

---

## Writing Your First Scenario

### 1. Create Scenario Class

```java
package com.drhiidden.fsj.scenarios;

import com.drhiidden.fsj.core.ScenarioRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;

@DisplayName("My Feature Scenarios")
class MyFeatureScenario extends ScenarioRunner {
    
    @Test
    @DisplayName("User does X → System responds Y")
    void myFirstScenario() {
        // GIVEN: Setup
        log.info("GIVEN: User is on homepage");
        
        // WHEN: Action
        var response = api.post("/api/my-resource")
            .withBody(Map.of("name", "Test"))
            .expectStatus(201)
            .execute();
        
        Long id = response.extractId();
        context.store("resource-id", id);
        log.info("WHEN: Created resource #{}", id);
        
        // THEN: Verify
        api.get("/api/my-resource/" + id)
            .expectStatus(200)
            .execute()
            .assertField("name", "Test");
        
        log.info("THEN: Resource retrieved successfully");
    }
    
    @Override
    protected void cleanup() {
        if (context.has("resource-id")) {
            api.delete("/api/my-resource/" + context.get("resource-id"))
                .execute();
            log.info("✓ Cleanup: Deleted test resource");
        }
    }
}
```

### 2. Run It

```bash
mvn test -Dtest=MyFeatureScenario
```

---

## Common Patterns

### Pattern 1: Create → Read → Verify

```java
// Create
Long id = api.post("/api/resource")
    .withBody(data)
    .expectStatus(201)
    .execute()
    .extractId();

// Read
var resource = api.get("/api/resource/" + id)
    .expectStatus(200)
    .execute()
    .asMap();

// Verify
assertEquals("expected", resource.get("field"));
```

### Pattern 2: Search → Filter → Assert

```java
// Search all
var results = api.get("/api/artists/search")
    .withQuery("q", "canserbero")
    .execute()
    .asList();

// Filter
var artist = results.stream()
    .filter(a -> "Canserbero".equals(a.get("name")))
    .findFirst()
    .orElseThrow();

// Assert
assertEquals("Venezuela", artist.get("country"));
```

### Pattern 3: Async Wait

```java
// Create
Long newsId = api.post("/api/news").execute().extractId();

// Wait for DB commit
waitForDbCommit();

// Verify (now reliable)
api.get("/api/news/" + newsId)
    .expectStatus(200)
    .execute();
```

### Pattern 4: Context Sharing

```java
// Step 1
Long artistId = api.post("/api/artists")...
context.store("artist-id", artistId);

// Step 2 (later in test)
Long id = context.get("artist-id");
api.get("/api/artists/" + id + "/albums")...
```

---

## Troubleshooting

### "Connection refused"

Backend not running:

```bash
# Check if running
curl http://localhost:8082/actuator/health

# Start backend
cd backend
mvn spring-boot:run
```

### "Tests failing randomly"

Likely async DB operations. Add wait:

```java
api.post("/api/resource")...execute();
waitForDbCommit(); // 100ms pause
api.get("/api/resource/" + id)...
```

### "Auth tests fail"

Update credentials in `test-dev.properties`:

```properties
admin.username=your_admin@email.com
admin.password=your_password
```

### "Expected 200 but got 404"

Resource doesn't exist yet. Either:

1. Create it first (in GIVEN block)
2. Use data that exists in DB
3. Check endpoint URL is correct

---

## Advanced Usage

### Custom Headers

```java
api.get("/api/protected")
    .withHeader("X-API-Key", apiKey)
    .withHeader("X-Request-ID", uuid)
    .execute();
```

### Performance Tracking

```java
context.startTiming("import");

api.post("/api/artists/import/batch")
    .withBody(batchData)
    .execute();

long duration = context.endTiming("import");

if (duration > 5000) {
    log.warn("Import took {}ms (expected < 5000ms)", duration);
}
```

### Complex Assertions

```java
response.execute()
    .assertField("status", "ACTIVE")
    .assertFieldNotNull("createdAt")
    .assertArrayNotEmpty("items")
    .assertArrayContains("id", expectedId);
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: SceneFlow Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run Tests
        run: |
          cd FSJ-Regressive
          mvn test
      
      - name: Upload Reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: FSJ-Regressive/target/surefire-reports/
```

---

## Next Steps

1. **Explore scenarios:**
   ```bash
   ls src/test/java/com/drhiidden/fsj/scenarios/
   ```

2. **Read scenario code:**
   - Start with `BackwardCompatibilityTest.java` (simplest)
   - Then `RegressionSmokeTest.java`
   - Finally `CMSWorkflowNoAuthScenario.java` (complete flows)

3. **Write your own:**
   - Copy existing scenario
   - Adapt to your use case
   - Run with `mvn test -Dtest=YourScenario`

4. **Integrate with CI/CD:**
   - See GitHub Actions example above
   - Exit code 0 = all pass, 1 = failures

---

**Framework:** SceneFlow v1.0.1  
**Status:** Production Ready ✅  
**Documentation:** See `README.md` for complete guide  

**Start testing journeys today.**
