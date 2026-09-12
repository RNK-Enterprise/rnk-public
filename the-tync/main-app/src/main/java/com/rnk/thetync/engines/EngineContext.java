package com.rnk.thetync.engines;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for engine operations.
 * Contains input data, configuration, and shared state.
 */
public class EngineContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Map<String, Object> config = new ConcurrentHashMap<>();

    public EngineContext() {}

    public EngineContext(Map<String, Object> initialData) {
        if (initialData != null) {
            data.putAll(initialData);
        }
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void setConfig(String key, Object value) {
        config.put(key, value);
    }

    public Object getConfig(String key) {
        return config.get(key);
    }

    public <T> T getConfig(String key, Class<T> type) {
        Object value = config.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public Map<String, Object> getAllData() {
        return new ConcurrentHashMap<>(data);
    }

    public Map<String, Object> getAllConfig() {
        return new ConcurrentHashMap<>(config);
    }
}