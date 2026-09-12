package com.rnk.thetync.engines;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitoring & Metrics Engine - Tracks performance with Micrometer and Reactor.
 */
public class MonitoringMetricsEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringMetricsEngine.class);

    private final String name = "MonitoringMetricsEngine";
    private final String version = "1.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;

    private final AtomicLong activeOperations = new AtomicLong(0);
    private Counter totalOperations;
    private Timer operationTimer;

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;

    public MonitoringMetricsEngine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getVersion() { return version; }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            logger.info("Initializing Monitoring & Metrics Engine");

            // Register metrics
            totalOperations = Counter.builder("tync.operations.total")
                    .description("Total number of operations")
                    .register(meterRegistry);

            operationTimer = Timer.builder("tync.operations.duration")
                    .description("Operation duration")
                    .register(meterRegistry);

            Gauge.builder("tync.operations.active", activeOperations, AtomicLong::get)
                    .description("Number of active operations")
                    .register(meterRegistry);

            health = EngineHealth.HEALTHY;
            initialized = true;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        if (!initialized) return Mono.error(new IllegalStateException("Engine not initialized"));

        activeOperations.incrementAndGet();

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            try {
                String metricType = context.get("metricType", String.class);
                String operation = context.get("operation", String.class);

                // Record metrics
                totalOperations.increment();
                sample.stop(operationTimer);

                Map<String, Object> result = Map.of(
                    "metricType", metricType,
                    "operation", operation,
                    "timestamp", System.currentTimeMillis(),
                    "activeOperations", activeOperations.get()
                );

                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(true, executionTime);

                return EngineResult.success(result, executionTime);
            } finally {
                activeOperations.decrementAndGet();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            health = EngineHealth.UNKNOWN;
            initialized = false;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public EngineHealth getHealth() { return health; }

    @Override
    public EngineMetrics getMetrics() { return metrics; }
}