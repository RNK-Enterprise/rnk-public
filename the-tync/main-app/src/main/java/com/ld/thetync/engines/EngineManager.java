package com.ld.thetync.engines;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all transformation engines in The Tync
 */
public class EngineManager {
    private static final Logger logger = LoggerFactory.getLogger(EngineManager.class);

    private final Map<String, Engine> engines = new ConcurrentHashMap<>();
    private final Map<String, EngineHealth> engineHealth = new ConcurrentHashMap<>();

    /**
     * Register an engine with the manager
     */
    public void registerEngine(Engine engine) {
        String name = engine.getClass().getSimpleName();
        engines.put(name, engine);
        engineHealth.put(name, new EngineHealth());
        logger.info("Registered engine: {}", name);
    }

    /**
     * Get an engine by name
     */
    public Optional<Engine> getEngine(String name) {
        return Optional.ofNullable(engines.get(name));
    }

    /**
     * Get all registered engines
     */
    public Collection<Engine> getAllEngines() {
        return engines.values();
    }

    /**
     * Get engine names
     */
    public Set<String> getEngineNames() {
        return engines.keySet();
    }

    /**
     * Check if an engine is healthy
     */
    public boolean isEngineHealthy(String name) {
        EngineHealth health = engineHealth.get(name);
        return health != null && health.isHealthy();
    }

    /**
     * Get engine health status
     */
    public EngineHealth getEngineHealth(String name) {
        return engineHealth.getOrDefault(name, new EngineHealth());
    }

    /**
     * Get the total number of registered engines
     */
    public int getEngineCount() {
        return engines.size();
    }
}