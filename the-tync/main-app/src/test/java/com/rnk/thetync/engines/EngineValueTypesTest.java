package com.rnk.thetync.engines;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the engine value types: {@link EngineResult},
 * {@link EngineMetrics}, {@link EngineContext}, and {@link EngineHealth}.
 */
class EngineValueTypesTest {

    // --- EngineResult ---

    @Test
    void engineResultSuccessCarriesDataAndTime() {
        EngineResult result = EngineResult.success("payload", 42L);
        assertTrue(result.success());
        assertEquals("payload", result.data());
        assertNull(result.errorMessage());
        assertEquals(42L, result.executionTimeMs());
    }

    @Test
    void engineResultFailureCarriesMessageAndNoData() {
        EngineResult result = EngineResult.failure("boom", 7L);
        assertFalse(result.success());
        assertNull(result.data());
        assertEquals("boom", result.errorMessage());
        assertEquals(7L, result.executionTimeMs());
    }

    // --- EngineMetrics ---

    @Test
    void engineMetricsStartAtZero() {
        EngineMetrics metrics = new EngineMetrics();
        assertEquals(0, metrics.getTotalExecutions());
        assertEquals(0, metrics.getTotalExecutionTimeMs());
        assertEquals(0.0, metrics.getSuccessRate());
        assertEquals(0, metrics.getAverageExecutionTimeMs());
    }

    @Test
    void engineMetricsRecordsSuccessfulExecution() {
        EngineMetrics metrics = new EngineMetrics();
        metrics.recordExecution(true, 10);
        assertEquals(1, metrics.getTotalExecutions());
        assertEquals(1, metrics.getSuccessfulExecutions());
        assertEquals(0, metrics.getFailedExecutions());
        assertEquals(1.0, metrics.getSuccessRate());
        assertEquals(10, metrics.getTotalExecutionTimeMs());
        assertEquals(10, metrics.getAverageExecutionTimeMs());
    }

    @Test
    void engineMetricsTracksMixedOutcomes() {
        EngineMetrics metrics = new EngineMetrics();
        metrics.recordExecution(true, 10);
        metrics.recordExecution(false, 30);
        assertEquals(2, metrics.getTotalExecutions());
        assertEquals(1, metrics.getSuccessfulExecutions());
        assertEquals(1, metrics.getFailedExecutions());
        assertEquals(0.5, metrics.getSuccessRate());
        assertEquals(40, metrics.getTotalExecutionTimeMs());
        assertEquals(20, metrics.getAverageExecutionTimeMs());
    }

    // --- EngineContext ---

    @Test
    void engineContextStoresAndRetrievesData() {
        EngineContext context = new EngineContext();
        context.put("key", "value");
        assertEquals("value", context.get("key"));
        assertEquals("value", context.get("key", String.class));
    }

    @Test
    void engineContextTypedGetReturnsNullOnTypeMismatch() {
        EngineContext context = new EngineContext();
        context.put("key", "value");
        assertNull(context.get("key", Integer.class));
    }

    @Test
    void engineContextTypedGetReturnsNullForMissingKey() {
        EngineContext context = new EngineContext();
        assertNull(context.get("missing", String.class));
    }

    @Test
    void engineContextInitialDataIsAvailable() {
        Map<String, Object> initial = new HashMap<>();
        initial.put("input", "mod.jar");
        EngineContext context = new EngineContext(initial);
        assertEquals("mod.jar", context.get("input"));
        assertEquals("mod.jar", context.getAllData().get("input"));
    }

    @Test
    void engineContextToleratesNullInitialMap() {
        EngineContext context = new EngineContext(null);
        assertNull(context.get("anything"));
    }

    @Test
    void engineContextConfigIsSeparateFromData() {
        EngineContext context = new EngineContext();
        context.put("dataKey", "dataValue");
        context.setConfig("configKey", "configValue");
        assertEquals("configValue", context.getConfig("configKey"));
        assertNull(context.get("configKey"));
        assertEquals("dataValue", context.getAllData().get("dataKey"));
        assertEquals("configValue", context.getAllConfig().get("configKey"));
    }

    @Test
    void engineContextTypedConfigGet() {
        EngineContext context = new EngineContext();
        context.setConfig("threads", 4);
        assertEquals(4, context.getConfig("threads", Integer.class));
        assertNull(context.getConfig("threads", String.class));
    }

    // --- EngineHealth ---

    @Test
    void engineHealthDefaultsToUnknown() {
        EngineHealth health = new EngineHealth();
        assertEquals(EngineHealth.Status.UNKNOWN, health.getStatus());
        assertFalse(health.isHealthy());
        assertEquals("Not checked yet", health.getDetails());
        assertTrue(health.getLastCheckTime() > 0);
    }

    @Test
    void engineHealthStaticConstantsCarryExpectedStatus() {
        assertTrue(EngineHealth.HEALTHY.isHealthy());
        assertEquals(EngineHealth.Status.HEALTHY, EngineHealth.HEALTHY.getStatus());
        assertFalse(EngineHealth.DEGRADED.isHealthy());
        assertFalse(EngineHealth.UNHEALTHY.isHealthy());
        assertEquals(EngineHealth.Status.UNKNOWN, EngineHealth.UNKNOWN.getStatus());
    }

    @Test
    void engineHealthCtorSetsStatusAndDetails() {
        EngineHealth health = new EngineHealth(EngineHealth.Status.DEGRADED, "slow");
        assertEquals(EngineHealth.Status.DEGRADED, health.getStatus());
        assertEquals("slow", health.getDetails());
        assertFalse(health.isHealthy());
    }

    @Test
    void engineHealthSettersUpdateState() {
        EngineHealth health = new EngineHealth();
        health.setStatus(EngineHealth.Status.HEALTHY);
        health.setDetails("all good");
        assertEquals(EngineHealth.Status.HEALTHY, health.getStatus());
        assertTrue(health.isHealthy());
        assertEquals("all good", health.getDetails());
    }

    @Test
    void engineHealthToStringContainsStatusAndDetails() {
        EngineHealth health = new EngineHealth(EngineHealth.Status.UNHEALTHY, "broken");
        String text = health.toString();
        assertNotNull(text);
        assertTrue(text.contains("UNHEALTHY"));
        assertTrue(text.contains("broken"));
    }
}
