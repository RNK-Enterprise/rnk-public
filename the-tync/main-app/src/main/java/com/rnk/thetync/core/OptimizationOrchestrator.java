package com.rnk.thetync.core;

import com.rnk.thetync.engines.Engine;
import com.rnk.thetync.engines.EngineManager;
import com.rnk.thetync.turbos.Turbo;
import com.rnk.thetync.turbos.TurboManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimization Orchestrator - Manages max optimization features
 * Enables comprehensive turbo activation, adaptive algorithm selection, and resource pooling
 */
public class OptimizationOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(OptimizationOrchestrator.class);

    private final MeterRegistry meterRegistry;
    private final EngineManager engineManager;
    private final TurboManager turboManager;
    private final ConcurrentHashMap<String, OptimizationProfile> activeProfiles = new ConcurrentHashMap<>();
    private final AtomicBoolean maxOptimizationEnabled = new AtomicBoolean(false);

    public OptimizationOrchestrator(MeterRegistry meterRegistry, EngineManager engineManager, TurboManager turboManager) {
        this.meterRegistry = meterRegistry;
        this.engineManager = engineManager;
        this.turboManager = turboManager;
    }

    /**
     * Initialize the optimization orchestrator
     */
    public void initialize() {
        logger.info("OptimizationOrchestrator initialized");
    }

    /**
     * Enable max optimization - activate all applicable turbos simultaneously
     */
    public Mono<Void> enableMaxOptimization() {
        return Mono.fromCallable(() -> {
            logger.info("Enabling max optimization mode");

            // Get all available turbos
            List<Turbo> allTurbos = new ArrayList<>(turboManager.getAllTurbos());

            // Activate all turbos in parallel
            List<Mono<Void>> activationMonos = allTurbos.stream()
                    .filter(turbo -> turbo.getHealth() != null && turbo.getHealth().isHealthy())
                    .map(turbo -> turbo.initialize()
                            .doOnSuccess(v -> logger.debug("Activated turbo: {}", turbo.getName()))
                            .doOnError(e -> logger.warn("Failed to activate turbo: {}", turbo.getName(), e)))
                    .toList();

            // Wait for all activations to complete
            return Mono.when(activationMonos)
                    .doOnSuccess(v -> {
                        maxOptimizationEnabled.set(true);
                        logger.info("Max optimization enabled - all turbos activated");
                    })
                    .doOnError(e -> logger.error("Failed to enable max optimization", e));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Disable max optimization
     */
    public Mono<Void> disableMaxOptimization() {
        return Mono.fromCallable(() -> {
            logger.info("Disabling max optimization mode");

            List<Turbo> allTurbos = new ArrayList<>(turboManager.getAllTurbos());

            List<Mono<Void>> shutdownMonos = allTurbos.stream()
                    .map(turbo -> turbo.shutdown()
                            .doOnSuccess(v -> logger.debug("Deactivated turbo: {}", turbo.getName()))
                            .doOnError(e -> logger.warn("Failed to deactivate turbo: {}", turbo.getName(), e)))
                    .toList();

            return Mono.when(shutdownMonos)
                    .doOnSuccess(v -> {
                        maxOptimizationEnabled.set(false);
                        activeProfiles.clear();
                        logger.info("Max optimization disabled");
                    })
                    .doOnError(e -> logger.error("Failed to disable max optimization", e));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Apply adaptive algorithm selection based on current workload
     */
    public Mono<OptimizationProfile> selectOptimalProfile(String workloadType) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return Mono.fromCallable(() -> {
            // Analyze current system state
            SystemMetrics metrics = gatherSystemMetrics();

            // Select optimal algorithms based on workload
            OptimizationProfile profile = createProfileForWorkload(workloadType, metrics);

            activeProfiles.put(workloadType, profile);

            long executionTime = System.nanoTime() - sample.stop(Timer.builder("orchestrator.profile.selection")
                    .tag("workload", workloadType)
                    .register(meterRegistry));

            logger.debug("Selected optimization profile for {}: {}", workloadType, profile.getName());

            return profile;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Check if max optimization is currently enabled
     */
    public boolean isMaxOptimizationEnabled() {
        return maxOptimizationEnabled.get();
    }

    /**
     * Get active optimization profiles
     */
    public List<OptimizationProfile> getActiveProfiles() {
        return List.copyOf(activeProfiles.values());
    }

    private SystemMetrics gatherSystemMetrics() {
        // Gather CPU, memory, and performance metrics
        Runtime runtime = Runtime.getRuntime();
        return new SystemMetrics(
                runtime.availableProcessors(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                meterRegistry
        );
    }

    private OptimizationProfile createProfileForWorkload(String workloadType, SystemMetrics metrics) {
        // Adaptive algorithm selection logic
        switch (workloadType.toLowerCase()) {
            case "mod-loading":
                return new OptimizationProfile("ModLoading-Optimized",
                        List.of("BytecodeTurbo", "CacheTurbo", "ValidationTurbo"),
                        OptimizationLevel.MAXIMUM);

            case "runtime-execution":
                return new OptimizationProfile("Runtime-Optimized",
                        List.of("InjectionTurbo", "SyncTurbo", "MetricsTurbo"),
                        OptimizationLevel.HIGH);

            case "memory-intensive":
                return new OptimizationProfile("Memory-Optimized",
                        List.of("CacheTurbo", "ReactiveTurbo", "BridgeTurbo"),
                        OptimizationLevel.BALANCED);

            default:
                return new OptimizationProfile("Default-Optimized",
                        List.of("BytecodeTurbo", "ValidationTurbo"),
                        OptimizationLevel.STANDARD);
        }
    }

    /**
     * System metrics for optimization decisions
     */
    public record SystemMetrics(int availableProcessors, long totalMemory, long freeMemory, MeterRegistry registry) {}

    /**
     * Optimization profile configuration
     */
    public static class OptimizationProfile {
        private final String name;
        private final List<String> activeTurbos;
        private final OptimizationLevel level;

        public OptimizationProfile(String name, List<String> activeTurbos, OptimizationLevel level) {
            this.name = name;
            this.activeTurbos = activeTurbos;
            this.level = level;
        }

        public String getName() { return name; }
        public List<String> getActiveTurbos() { return activeTurbos; }
        public OptimizationLevel getLevel() { return level; }
    }

    /**
     * Optimization levels
     */
    public enum OptimizationLevel {
        STANDARD, BALANCED, HIGH, MAXIMUM
    }
}