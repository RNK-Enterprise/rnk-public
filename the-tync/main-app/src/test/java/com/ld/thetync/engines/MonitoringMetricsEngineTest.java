package com.ld.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MonitoringMetricsEngine}: lifecycle, metric recording,
 * and full coverage of every executable line and branch.
 */
class MonitoringMetricsEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private MonitoringMetricsEngine newEngine() {
        return new MonitoringMetricsEngine(new SimpleMeterRegistry());
    }

    private MonitoringMetricsEngine initializedEngine() {
        MonitoringMetricsEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);
        return engine;
    }

    @Test
    void executeBeforeInitializeFails() {
        MonitoringMetricsEngine engine = newEngine();
        EngineContext context = new EngineContext();
        context.put("metricType", "counter");
        context.put("operation", "test");

        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }

    @Test
    void initializeRegistersMetricsAndReportsHealthy() {
        MonitoringMetricsEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);

        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertEquals("MonitoringMetricsEngine", engine.getName());
        assertEquals("1.0.0", engine.getVersion());
        assertTrue(engine.getMetrics() == engine.getMetrics());
    }

    @Test
    void executeRecordsOperationAndReturnsDetails() {
        MonitoringMetricsEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("metricType", "timer");
        context.put("operation", "record");

        EngineResult result = engine.execute(context).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals("timer", data.get("metricType"));
        assertEquals("record", data.get("operation"));
        // The result snapshot is taken while this operation is still in flight.
        assertEquals(1L, data.get("activeOperations"));
    }

    @Test
    void executeSequenceBalancesActiveCount() {
        MonitoringMetricsEngine engine = initializedEngine();

        for (int i = 0; i < 3; i++) {
            EngineContext context = new EngineContext();
            context.put("metricType", "t" + i);
            context.put("operation", "op" + i);
            EngineResult result = engine.execute(context).block(TIMEOUT);

            assertTrue(result.success());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.data();
            // Snapshot is taken mid-execution: this operation is counted,
            // previous ones have already been released.
            assertEquals(1L, data.get("activeOperations"));
        }
    }

    @Test
    void shutdownResetsState() {
        MonitoringMetricsEngine engine = initializedEngine();
        engine.shutdown().block(TIMEOUT);

        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());

        EngineContext context = new EngineContext();
        context.put("metricType", "t");
        context.put("operation", "o");
        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }
}
