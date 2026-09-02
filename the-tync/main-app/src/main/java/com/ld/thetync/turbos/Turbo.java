package com.ld.thetync.turbos;

import reactor.core.publisher.Mono;

/**
 * Core interface for all Tync turbos.
 * Turbos are high-performance acceleration modules that turbocharge specific transformation pathways.
 * Each turbo implements reactive execution with quantum-inspired optimizations.
 */
public interface Turbo {

    /**
     * Gets the unique name of this turbo.
     * @return the turbo name
     */
    String getName();

    /**
     * Gets the version of this turbo.
     * @return the turbo version
     */
    String getVersion();

    /**
     * Initializes the turbo with required dependencies and configuration.
     * @return a Mono that completes when initialization is done
     */
    Mono<Void> initialize();

    /**
     * Executes the turbo's accelerated function.
     * @param context the execution context containing input data
     * @return a Mono containing the execution result
     */
    Mono<TurboResult> execute(TurboContext context);

    /**
     * Shuts down the turbo and releases resources.
     * @return a Mono that completes when shutdown is done
     */
    Mono<Void> shutdown();

    /**
     * Gets the current health status of the turbo.
     * @return the health status
     */
    default TurboHealth getHealth() {
        return new TurboHealth(TurboHealth.Status.HEALTHY, "Turbo is healthy");
    }

    /**
     * Gets performance metrics for this turbo.
     * @return the metrics
     */
    TurboMetrics getMetrics();

    /**
     * Gets the acceleration factor provided by this turbo.
     * @return the acceleration factor (e.g., 10.0 for 10x speedup)
     */
    double getAccelerationFactor();
}
