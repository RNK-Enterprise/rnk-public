package com.ld.thetync;

import com.ld.thetync.turbos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for all Tync turbos
 */
public class TurboManager {
    private static final Logger logger = LoggerFactory.getLogger(TurboManager.class);

    private final Map<String, Turbo> turbos = new ConcurrentHashMap<>();

    public void registerTurbo(Turbo turbo) {
        turbos.put(turbo.getName(), turbo);
        logger.info("Registered turbo: {}", turbo.getName());
    }

    public Turbo getTurbo(String name) {
        return turbos.get(name);
    }

    public Map<String, Turbo> getAllTurbos() {
        return new ConcurrentHashMap<>(turbos);
    }

    public void shutdownAll() {
        for (Turbo turbo : turbos.values()) {
            try {
                turbo.shutdown().block();
            } catch (Exception e) {
                logger.error("Error shutting down turbo {}", turbo.getName(), e);
            }
        }
        turbos.clear();
    }
}