package com.ld.thetync.turbos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for turbo operations.
 * Contains input data, configuration, and shared state with performance optimizations.
 */
public class TurboContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private volatile double accelerationFactor = 1.0;

    public TurboContext() {}

    public TurboContext(Map<String, Object> initialData) {
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

    public void setAccelerationFactor(double factor) {
        this.accelerationFactor = factor;
    }

    public double getAccelerationFactor() {
        return accelerationFactor;
    }

    public Map<String, Object> getAllData() {
        return new ConcurrentHashMap<>(data);
    }

    public Map<String, Object> getAllConfig() {
        return new ConcurrentHashMap<>(config);
    }
}
