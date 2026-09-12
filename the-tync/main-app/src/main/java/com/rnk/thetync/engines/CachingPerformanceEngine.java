package com.rnk.thetync.engines;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Caching & Performance Engine - Optimizes transformations using Caffeine and Guava.
 */
public class CachingPerformanceEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(CachingPerformanceEngine.class);

    private final String name = "CachingPerformanceEngine";
    private final String version = "1.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;

    private final Cache<String, Object> performanceCache;

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;

    public CachingPerformanceEngine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.performanceCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getVersion() { return version; }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            logger.info("Initializing Caching & Performance Engine");
            health = EngineHealth.HEALTHY;
            initialized = true;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        if (!initialized) return Mono.error(new IllegalStateException("Engine not initialized"));

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            String cacheKey = context.get("cacheKey", String.class);
            Object data = context.get("data");

            Object result = performanceCache.get(cacheKey, k -> data);

            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordExecution(true, executionTime);

            sample.stop(Timer.builder("engine.execution").tag("engine", name).register(meterRegistry));

            return EngineResult.success(result, executionTime);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            performanceCache.invalidateAll();
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