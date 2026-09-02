package com.ld.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CachingPerformanceEngine}: lifecycle, cache semantics,
 * and full coverage of every executable line and branch.
 */
class CachingPerformanceEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private CachingPerformanceEngine newEngine() {
        return new CachingPerformanceEngine(new SimpleMeterRegistry());
    }

    private CachingPerformanceEngine initializedEngine() {
        CachingPerformanceEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);
        return engine;
    }

    @Test
    void executeBeforeInitializeFails() {
        CachingPerformanceEngine engine = newEngine();
        EngineContext context = new EngineContext();
        context.put("cacheKey", "key");
        context.put("data", "value");

        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        CachingPerformanceEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);

        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertEquals("CachingPerformanceEngine", engine.getName());
        assertEquals("1.0.0", engine.getVersion());
        assertTrue(engine.getMetrics() == engine.getMetrics());
    }

    @Test
    void executeStoresAndReturnsData() {
        CachingPerformanceEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("cacheKey", "k1");
        context.put("data", "payload");

        EngineResult result = engine.execute(context).block(TIMEOUT);

        assertTrue(result.success());
        assertEquals("payload", result.data());
    }

    @Test
    void executeReturnsCachedValueForRepeatedKey() {
        CachingPerformanceEngine engine = initializedEngine();

        EngineContext first = new EngineContext();
        first.put("cacheKey", "k2");
        first.put("data", "original");
        EngineResult firstResult = engine.execute(first).block(TIMEOUT);

        EngineContext second = new EngineContext();
        second.put("cacheKey", "k2");
        second.put("data", "replacement");
        EngineResult secondResult = engine.execute(second).block(TIMEOUT);

        assertTrue(firstResult.success());
        assertTrue(secondResult.success());
        assertEquals("original", secondResult.data());
        assertNotSame("replacement", secondResult.data());
    }

    @Test
    void distinctKeysHoldDistinctValues() {
        CachingPerformanceEngine engine = initializedEngine();

        EngineContext a = new EngineContext();
        a.put("cacheKey", "a");
        a.put("data", "va");
        EngineResult aResult = engine.execute(a).block(TIMEOUT);

        EngineContext b = new EngineContext();
        b.put("cacheKey", "b");
        b.put("data", "vb");
        EngineResult bResult = engine.execute(b).block(TIMEOUT);

        assertEquals("va", aResult.data());
        assertEquals("vb", bResult.data());
    }

    @Test
    void shutdownResetsState() {
        CachingPerformanceEngine engine = initializedEngine();
        engine.shutdown().block(TIMEOUT);

        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());

        EngineContext context = new EngineContext();
        context.put("cacheKey", "k");
        context.put("data", "v");
        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }
}
