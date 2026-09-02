package com.ld.thetync.turbos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all performance turbos in The Tync
 */
public class TurboManager {
    private static final Logger logger = LoggerFactory.getLogger(TurboManager.class);

    private final Map<String, Turbo> turbos = new ConcurrentHashMap<>();
    private final Map<String, TurboHealth> turboHealth = new ConcurrentHashMap<>();

    /**
     * Register a turbo with the manager
     */
    public void registerTurbo(Turbo turbo) {
        String name = turbo.getClass().getSimpleName();
        turbos.put(name, turbo);
        turboHealth.put(name, new TurboHealth());
        logger.info("Registered turbo: {}", name);
    }

    /**
     * Get a turbo by name
     */
    public Optional<Turbo> getTurbo(String name) {
        return Optional.ofNullable(turbos.get(name));
    }

    /**
     * Get all registered turbos
     */
    public Collection<Turbo> getAllTurbos() {
        return turbos.values();
    }

    /**
     * Get turbo names
     */
    public Set<String> getTurboNames() {
        return turbos.keySet();
    }

    /**
     * Check if a turbo is healthy
     */
    public boolean isTurboHealthy(String name) {
        TurboHealth health = turboHealth.get(name);
        return health != null && health.isHealthy();
    }

    /**
     * Get turbo health status
     */
    public TurboHealth getTurboHealth(String name) {
        return turboHealth.getOrDefault(name, new TurboHealth());
    }

    /**
     * Get the total number of registered turbos
     */
    public int getTurboCount() {
        return turbos.size();
    }
}
