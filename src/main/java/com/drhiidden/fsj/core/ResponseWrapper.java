package com.drhiidden.fsj.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Wrapper sobre Response de Rest-Assured con assertions fluidas.
 */
@Slf4j
@RequiredArgsConstructor
public class ResponseWrapper {
    
    private final Response response;
    private final ObjectMapper objectMapper;
    
    public int getStatus() {
        return response.getStatusCode();
    }
    
    public String getBody() {
        return response.body().asString();
    }
    
    public <T> T as(Class<T> type) {
        try {
            return objectMapper.readValue(getBody(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response as " + type.getSimpleName(), e);
        }
    }
    
    public <T> T as(TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(getBody(), typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }
    
    public Map<String, Object> asMap() {
        return as(new TypeReference<>() {});
    }
    
    public List<Map<String, Object>> asList() {
        return as(new TypeReference<>() {});
    }
    
    /**
     * Extrae el ID de un recurso creado (campo "id" del JSON root).
     */
    public Long extractId() {
        Object id = response.jsonPath().get("id");
        if (id == null) {
            throw new AssertionError("Response does not contain 'id' field");
        }
        return id instanceof Integer ? ((Integer) id).longValue() : (Long) id;
    }
    
    /**
     * Extrae un campo del JSON response usando JsonPath.
     * 
     * @param path JsonPath expression (ej: "content[0].title", "data.id")
     */
    @SuppressWarnings("unchecked")
    public <T> T extract(String path) {
        return (T) response.jsonPath().get(path);
    }
    
    /**
     * Assertions fluidas sobre el response.
     */
    public ResponseWrapper assertField(String jsonPath, Object expectedValue) {
        Object actual = extract(jsonPath);
        if (!expectedValue.equals(actual)) {
            throw new AssertionError(String.format(
                "Field '%s' mismatch: expected '%s' but was '%s'",
                jsonPath, expectedValue, actual
            ));
        }
        log.debug("  ✓ Field '{}' = {}", jsonPath, expectedValue);
        return this;
    }
    
    public ResponseWrapper assertFieldNotNull(String jsonPath) {
        Object value = extract(jsonPath);
        if (value == null) {
            throw new AssertionError("Field '" + jsonPath + "' is null");
        }
        log.debug("  ✓ Field '{}' is not null", jsonPath);
        return this;
    }
    
    public ResponseWrapper assertFieldExists(String jsonPath) {
        try {
            response.jsonPath().get(jsonPath);
            log.debug("  ✓ Field '{}' exists", jsonPath);
        } catch (Exception e) {
            throw new AssertionError("Field '" + jsonPath + "' does not exist in response");
        }
        return this;
    }
    
    public ResponseWrapper assertArraySize(String jsonPath, int expectedSize) {
        List<?> array = extract(jsonPath);
        if (array == null || array.size() != expectedSize) {
            throw new AssertionError(String.format(
                "Array '%s' size mismatch: expected %d but was %d",
                jsonPath, expectedSize, array != null ? array.size() : 0
            ));
        }
        log.debug("  ✓ Array '{}' size = {}", jsonPath, expectedSize);
        return this;
    }
    
    public ResponseWrapper assertArrayNotEmpty(String jsonPath) {
        List<?> array = extract(jsonPath);
        if (array == null || array.isEmpty()) {
            throw new AssertionError("Array '" + jsonPath + "' is empty or null");
        }
        log.debug("  ✓ Array '{}' not empty (size: {})", jsonPath, array.size());
        return this;
    }
    
    public ResponseWrapper assertContains(String jsonPath, Object value) {
        List<?> array = extract(jsonPath);
        if (array == null || !array.contains(value)) {
            throw new AssertionError(String.format(
                "Array '%s' does not contain value '%s'",
                jsonPath, value
            ));
        }
        log.debug("  ✓ Array '{}' contains {}", jsonPath, value);
        return this;
    }
    
    public ResponseWrapper assertStatus(int expected) {
        int actual = getStatus();
        if (actual != expected) {
            throw new AssertionError(String.format(
                "Status mismatch: expected %d but was %d. Body: %s",
                expected, actual, getBody()
            ));
        }
        return this;
    }
    
    /**
     * Debug: imprime el response completo.
     */
    public ResponseWrapper printResponse() {
        log.info("Response [{}]: {}", getStatus(), getBody());
        return this;
    }
}
