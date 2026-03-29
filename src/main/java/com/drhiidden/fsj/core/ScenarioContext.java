package com.drhiidden.fsj.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contexto compartido entre steps de un escenario.
 * Permite pasar datos entre pasos (IDs creados, tokens, etc.).
 * 
 * Thread-safe para ejecución paralela de escenarios.
 */
@Slf4j
public class ScenarioContext {
    
    @Getter
    private final String scenarioName;
    private final Map<String, Object> context = new HashMap<>();
    private final Map<String, Long> timings = new HashMap<>();
    
    public ScenarioContext(String scenarioName) {
        this.scenarioName = scenarioName;
    }
    
    /**
     * Guarda un valor en el contexto.
     */
    public <T> void store(String key, T value) {
        context.put(key, value);
        log.debug("  [Context] Stored '{}' = {}", key, value);
    }
    
    /**
     * Recupera un valor del contexto.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = context.get(key);
        if (value == null) {
            throw new IllegalStateException("Key '" + key + "' not found in scenario context");
        }
        return (T) value;
    }
    
    /**
     * Recupera un valor opcional.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(String key) {
        return Optional.ofNullable((T) context.get(key));
    }
    
    /**
     * Verifica si existe una clave.
     */
    public boolean has(String key) {
        return context.containsKey(key);
    }
    
    /**
     * Marca el inicio de una operación para medir tiempo.
     */
    public void startTiming(String operation) {
        timings.put(operation, System.currentTimeMillis());
    }
    
    /**
     * Marca el fin de una operación y retorna duración en ms.
     */
    public long endTiming(String operation) {
        Long start = timings.get(operation);
        if (start == null) {
            throw new IllegalStateException("Timing '" + operation + "' was not started");
        }
        long duration = System.currentTimeMillis() - start;
        log.debug("  [Timing] {} took {}ms", operation, duration);
        return duration;
    }
    
    /**
     * Limpia el contexto (útil para cleanup en @AfterEach).
     */
    public void clear() {
        context.clear();
        timings.clear();
    }
    
    /**
     * Debug: imprime todo el contexto.
     */
    public void printContext() {
        log.info("Scenario Context [{}]:", scenarioName);
        context.forEach((k, v) -> log.info("  {} = {}", k, v));
    }
}
