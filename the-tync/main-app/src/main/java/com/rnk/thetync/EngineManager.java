package com.rnk.thetync;

import com.rnk.thetync.engines.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for all Tync engines
 */
public class EngineManager {
    private static final Logger logger = LoggerFactory.getLogger(EngineManager.class);

    private final Map<String, Engine> engines = new ConcurrentHashMap<>();

    public void registerEngine(Engine engine) {
        engines.put(engine.getName(), engine);
        logger.info("Registered engine: {}", engine.getName());
    }

    public Engine getEngine(String name) {
        return engines.get(name);
    }

    public Map<String, Engine> getAllEngines() {
        return new ConcurrentHashMap<>(engines);
    }

    public void shutdownAll() {
        for (Engine engine : engines.values()) {
            try {
                engine.shutdown().block();
            } catch (Exception e) {
                logger.error("Error shutting down engine {}", engine.getName(), e);
            }
        }
        engines.clear();
    }
}