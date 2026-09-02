package com.ld.thetync.engines;

import reactor.core.publisher.Mono;

/**
 * Core interface for all Tync engines.
 * Engines are specialized components that handle specific transformation and compatibility tasks.
 * Each engine implements a reactive execution model for async processing.
 */
public interface Engine {

    /**
     * Gets the unique name of this engine.
     * @return the engine name
     */
    String getName();

    /**
     * Gets the version of this engine.
     * @return the engine version
     */
    String getVersion();

    /**
     * Initializes the engine with required dependencies and configuration.
     * @return a Mono that completes when initialization is done
     */
    Mono<Void> initialize();

    /**
     * Executes the engine's primary function.
     * @param context the execution context containing input data
     * @return a Mono containing the execution result
     */
    Mono<EngineResult> execute(EngineContext context);

    /**
     * Shuts down the engine and releases resources.
     * @return a Mono that completes when shutdown is done
     */
    Mono<Void> shutdown();

    /**
     * Gets the current health status of the engine.
     * @return the health status
     */
    default EngineHealth getHealth() {
        return new EngineHealth(EngineHealth.Status.HEALTHY, "Engine is healthy");
    }

    /**
     * Gets performance metrics for this engine.
     * @return the metrics
     */
    EngineMetrics getMetrics();
}