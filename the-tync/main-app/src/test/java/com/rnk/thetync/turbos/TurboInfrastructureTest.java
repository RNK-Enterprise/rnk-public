package com.rnk.thetync.turbos;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the turbo infrastructure: {@link TurboContext},
 * {@link TurboMetrics}, {@link TurboResult} and {@link TurboHealth}.
 * The registry is covered by {@code com.rnk.thetync.TurboManagerTest}.
 */
class TurboInfrastructureTest {

    /**
     * Minimal turbo used to exercise manager and interface defaults.
     */
    private static class TestTurbo implements Turbo {
        @Override
        public String getName() {
            return "TestTurbo";
        }

        @Override
        public String getVersion() {
            return "1.0.0-test";
        }

        @Override
        public reactor.core.publisher.Mono<Void> initialize() {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<TurboResult> execute(TurboContext context) {
            return reactor.core.publisher.Mono.just(TurboResult.success(null, 0L, 1.0));
        }

        @Override
        public reactor.core.publisher.Mono<Void> shutdown() {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public TurboMetrics getMetrics() {
            return new TurboMetrics();
        }

        @Override
        public TurboHealth getHealth() {
            return TurboHealth.HEALTHY;
        }

        @Override
        public double getAccelerationFactor() {
            return 2.0;
        }
    }

    private static class OtherTurbo extends TestTurbo {
        @Override
        public String getName() {
            return "OtherTurbo";
        }
    }

    // --- TurboContext ---

    @Test
    void turboContextStoresAndRetrievesData() {
        TurboContext context = new TurboContext();
        context.put("key", "value");
        assertEquals("value", context.get("key"));
        assertEquals("value", context.get("key", String.class));
    }

    @Test
    void turboContextTypedGetReturnsNullOnMismatchOrMissing() {
        TurboContext context = new TurboContext();
        context.put("key", "value");
        assertNull(context.get("key", Integer.class));
        assertNull(context.get("missing", String.class));
    }

    @Test
    void turboContextInitialDataIsAvailable() {
        Map<String, Object> initial = new HashMap<>();
        initial.put("input", "classes");
        TurboContext context = new TurboContext(initial);
        assertEquals("classes", context.get("input"));
        assertEquals("classes", context.getAllData().get("input"));
    }

    @Test
    void turboContextToleratesNullInitialMap() {
        TurboContext context = new TurboContext(null);
        assertNull(context.get("anything"));
    }

    @Test
    void turboContextConfigIsSeparateFromData() {
        TurboContext context = new TurboContext();
        context.put("dataKey", "dataValue");
        context.setConfig("configKey", "configValue");
        assertEquals("configValue", context.getConfig("configKey"));
        assertNull(context.get("configKey"));
        assertEquals("dataValue", context.getAllData().get("dataKey"));
        assertEquals("configValue", context.getAllConfig().get("configKey"));
    }

    @Test
    void turboContextTypedConfigGet() {
        TurboContext context = new TurboContext();
        context.setConfig("threads", 8);
        assertEquals(8, context.getConfig("threads", Integer.class));
        assertNull(context.getConfig("threads", String.class));
    }

    @Test
    void turboContextAccelerationFactorDefaultsToOne() {
        TurboContext context = new TurboContext();
        assertEquals(1.0, context.getAccelerationFactor());
    }

    @Test
    void turboContextAccelerationFactorIsSettable() {
        TurboContext context = new TurboContext();
        context.setAccelerationFactor(12.5);
        assertEquals(12.5, context.getAccelerationFactor());
    }

    // --- TurboMetrics ---

    @Test
    void turboMetricsStartAtZero() {
        TurboMetrics metrics = new TurboMetrics();
        assertEquals(0, metrics.getTotalExecutions());
        assertEquals(0, metrics.getSuccessfulExecutions());
        assertEquals(0, metrics.getFailedExecutions());
        assertEquals(0.0, metrics.getSuccessRate());
        assertEquals(0, metrics.getTotalExecutionTimeMs());
        assertEquals(0, metrics.getAverageExecutionTimeMs());
        assertEquals(0.0, metrics.getAverageAccelerationFactor());
    }

    @Test
    void turboMetricsRecordsSuccessfulExecution() {
        TurboMetrics metrics = new TurboMetrics();
        metrics.recordExecution(true, 10, 5.0);
        assertEquals(1, metrics.getTotalExecutions());
        assertEquals(1, metrics.getSuccessfulExecutions());
        assertEquals(0, metrics.getFailedExecutions());
        assertEquals(1.0, metrics.getSuccessRate());
        assertEquals(10, metrics.getTotalExecutionTimeMs());
        assertEquals(10, metrics.getAverageExecutionTimeMs());
        assertEquals(5.0, metrics.getAverageAccelerationFactor());
    }

    @Test
    void turboMetricsTracksMixedOutcomesAndAverages() {
        TurboMetrics metrics = new TurboMetrics();
        metrics.recordExecution(true, 10, 10.0);
        metrics.recordExecution(false, 30, 2.5);
        assertEquals(2, metrics.getTotalExecutions());
        assertEquals(1, metrics.getSuccessfulExecutions());
        assertEquals(1, metrics.getFailedExecutions());
        assertEquals(0.5, metrics.getSuccessRate());
        assertEquals(40, metrics.getTotalExecutionTimeMs());
        assertEquals(20, metrics.getAverageExecutionTimeMs());
        // Milli-factor storage: (10000 + 2500) / 2 = 6250 milli => 6.25
        assertEquals(6.25, metrics.getAverageAccelerationFactor());
    }

    // --- TurboResult ---

    @Test
    void turboResultSuccessCarriesAllFields() {
        TurboResult result = TurboResult.success("payload", 12L, 3.5);
        assertTrue(result.isSuccess());
        assertEquals("payload", result.getData());
        assertNull(result.getErrorMessage());
        assertEquals(12L, result.getExecutionTimeMs());
        assertEquals(3.5, result.getAccelerationFactor());
    }

    @Test
    void turboResultFailureCarriesErrorAndNoData() {
        TurboResult result = TurboResult.failure("boom", 4L, 1.0);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("boom", result.getErrorMessage());
        assertEquals(4L, result.getExecutionTimeMs());
        assertEquals(1.0, result.getAccelerationFactor());
    }

    // --- TurboHealth ---

    @Test
    void turboHealthDeclaresStatusesInOrder() {
        assertArrayEquals(
            new TurboHealth[] {
                TurboHealth.HEALTHY, TurboHealth.DEGRADED, TurboHealth.UNHEALTHY,
                TurboHealth.UNKNOWN, TurboHealth.OVERHEATED, TurboHealth.THROTTLED
            },
            TurboHealth.values());
        assertEquals(TurboHealth.THROTTLED, TurboHealth.valueOf("THROTTLED"));
    }
}
