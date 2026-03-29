package com.drhiidden.fsj.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente HTTP fluido para interactuar con APIs REST.
 * Proporciona un DSL declarativo para construir requests y validar responses.
 * 
 * Ejemplo:
 * <pre>
 * var client = new ApiClient("http://localhost:8082");
 * var response = client.post("/api/news")
 *     .withAuth(token)
 *     .withBody(newsData)
 *     .expectStatus(201)
 *     .assertField("title", "Mi noticia")
 *     .execute();
 * </pre>
 */
@Slf4j
public class ApiClient {
    
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private String authToken;
    
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        RestAssured.baseURI = baseUrl;
    }
    
    public ApiClient withAuth(String token) {
        this.authToken = token;
        return this;
    }
    
    public RequestBuilder get(String path) {
        return new RequestBuilder("GET", path);
    }
    
    public RequestBuilder post(String path) {
        return new RequestBuilder("POST", path);
    }
    
    public RequestBuilder put(String path) {
        return new RequestBuilder("PUT", path);
    }
    
    public RequestBuilder patch(String path) {
        return new RequestBuilder("PATCH", path);
    }
    
    public RequestBuilder delete(String path) {
        return new RequestBuilder("DELETE", path);
    }
    
    /**
     * Builder fluido para construir y ejecutar requests.
     */
    public class RequestBuilder {
        private final String method;
        private final String path;
        private final Map<String, Object> headers = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        private Object body;
        private Integer expectedStatus;
        
        private RequestBuilder(String method, String path) {
            this.method = method;
            this.path = path;
        }
        
        public RequestBuilder withAuth(String token) {
            headers.put("Authorization", "Bearer " + token);
            return this;
        }
        
        public RequestBuilder withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }
        
        public RequestBuilder withQuery(String name, Object value) {
            queryParams.put(name, value);
            return this;
        }
        
        public RequestBuilder withBody(Object body) {
            this.body = body;
            return this;
        }
        
        public RequestBuilder expectStatus(int status) {
            this.expectedStatus = status;
            return this;
        }
        
        /**
         * Ejecuta el request y retorna ResponseWrapper para assertions.
         */
        public ResponseWrapper execute() {
            log.info("▶ {} {} (expected: {})", method, path, expectedStatus != null ? expectedStatus : "any");
            
            RequestSpecification spec = RestAssured.given()
                .contentType(ContentType.JSON);
            
            // Auth global si existe
            if (authToken != null && !headers.containsKey("Authorization")) {
                spec.header("Authorization", "Bearer " + authToken);
            }
            
            // Headers custom
            headers.forEach((k, v) -> spec.header(k, String.valueOf(v)));
            
            // Query params
            queryParams.forEach((k, v) -> spec.queryParam(k, v));
            
            // Body
            if (body != null) {
                try {
                    String json = objectMapper.writeValueAsString(body);
                    spec.body(json);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize body", e);
                }
            }
            
            // Execute
            Response response = switch (method) {
                case "GET" -> spec.get(path);
                case "POST" -> spec.post(path);
                case "PUT" -> spec.put(path);
                case "PATCH" -> spec.patch(path);
                case "DELETE" -> spec.delete(path);
                default -> throw new IllegalArgumentException("Unsupported method: " + method);
            };
            
            int actualStatus = response.getStatusCode();
            
            // Validar status esperado
            if (expectedStatus != null && actualStatus != expectedStatus) {
                log.error("✗ Status mismatch: expected {}, got {}", expectedStatus, actualStatus);
                log.error("Response body: {}", response.body().asString());
                throw new AssertionError(String.format(
                    "Expected status %d but got %d for %s %s. Body: %s",
                    expectedStatus, actualStatus, method, path, response.body().asString()
                ));
            }
            
            log.info("✓ {} (status: {})", path, actualStatus);
            return new ResponseWrapper(response, objectMapper);
        }
    }
}
