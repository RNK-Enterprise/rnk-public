package com.rnk.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link NetworkSynchronizationEngine}. All HTTP traffic is served
 * by an in-process interceptor injected into the engine's client, so every
 * line and branch is covered without touching a real network.
 */
class NetworkSynchronizationEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private NetworkSynchronizationEngine engineWith(Function<Interceptor.Chain, Response> handler,
                                                    AtomicInteger hits) {
        NetworkSynchronizationEngine engine = new NetworkSynchronizationEngine(new SimpleMeterRegistry());
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    hits.incrementAndGet();
                    // The /health path always answers OK so tests that target
                    // other endpoints can get past initialize().
                    if (chain.request().url().encodedPath().endsWith("/health")) {
                        return respond(chain, 200, "{}");
                    }
                    return handler.apply(chain);
                })
                .build();
        try {
            Field field = NetworkSynchronizationEngine.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(engine, client);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not inject test HTTP client", e);
        }
        return engine;
    }

    private Response respond(Interceptor.Chain chain, int code, String body) {
        return new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    /**
     * Builds a successful response with a null body, which the engine must
     * handle without attempting to read content.
     */
    private Response respondNoBody(Interceptor.Chain chain, int code) {
        return new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test-no-body")
                .build();
    }

    private EngineContext contextFor(String operation, String modId, Map<String, Object> data) {
        EngineContext context = new EngineContext();
        if (operation != null) context.put("operation", operation);
        if (modId != null) context.put("modId", modId);
        if (data != null) context.put("data", data);
        return context;
    }

    @Test
    void executeBeforeInitializeFails() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), new AtomicInteger());

        assertThrows(IllegalStateException.class,
                () -> engine.execute(contextFor("sync_cache", null, null)).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthyWhenHealthEndpointSucceeds() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), new AtomicInteger());

        engine.initialize().block(TIMEOUT);

        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertEquals("NetworkSynchronizationEngine", engine.getName());
        assertEquals("1.0.0", engine.getVersion());
        assertTrue(engine.getMetrics() == engine.getMetrics());
    }

    @Test
    void initializeMarksUnhealthyWhenHealthEndpointFails() {
        NetworkSynchronizationEngine engine = new NetworkSynchronizationEngine(new SimpleMeterRegistry());
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> respond(chain, 500, "{}"))
                .build();
        try {
            Field field = NetworkSynchronizationEngine.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(engine, client);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not inject test HTTP client", e);
        }

        RuntimeException ex = assertThrows(RuntimeException.class, () -> engine.initialize().block(TIMEOUT));
        assertTrue(ex.getCause() instanceof java.io.IOException);
        assertEquals(EngineHealth.UNHEALTHY, engine.getHealth());
    }

    @Test
    void fetchCompatibilityFetchesThenServesFromCache() {
        AtomicInteger hits = new AtomicInteger();
        NetworkSynchronizationEngine engine = engineWith(
                chain -> respond(chain, 200, "{\"status\":\"ok\"}"), hits);
        engine.initialize().block(TIMEOUT);
        int baseline = hits.get();

        EngineResult first = engine.execute(contextFor("fetch_compatibility", "mymod", null)).block(TIMEOUT);
        int hitsAfterFirst = hits.get();

        EngineResult second = engine.execute(contextFor("fetch_compatibility", "mymod", null)).block(TIMEOUT);

        assertTrue(first.success());
        assertTrue(second.success());
        assertEquals(1, hitsAfterFirst - baseline);
        assertEquals(hitsAfterFirst, hits.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) second.data();
        assertEquals("ok", data.get("status"));
    }

    @Test
    void fetchCompatibilityFailureReturnsFailureResult() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 404, "{}"), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("fetch_compatibility", "ghostmod", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Failed to fetch compatibility data: 404", result.errorMessage());
    }

    @Test
    void submitResultReportsServerResponse() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 201, "{\"accepted\":true}"),
                new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(
                contextFor("submit_result", "mymod", Map.of("compat", true))).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals(true, data.get("success"));
        assertEquals(201, data.get("statusCode"));
        assertEquals("{\"accepted\":true}", data.get("response"));
    }

    @Test
    void getStatisticsParsesResponse() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{\"mods\":42}"),
                new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("get_statistics", null, null)).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals(42, data.get("mods"));
    }

    @Test
    void getStatisticsFailureReturnsFailureResult() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 503, "{}"), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("get_statistics", null, null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Failed to fetch statistics: 503", result.errorMessage());
    }

    @Test
    void syncCacheWorksWithoutNetwork() {
        AtomicInteger hits = new AtomicInteger();
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), hits);
        engine.initialize().block(TIMEOUT);
        int baseline = hits.get();

        EngineResult result = engine.execute(contextFor("sync_cache", null, null)).block(TIMEOUT);

        assertTrue(result.success());
        assertEquals(baseline, hits.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals("completed", data.get("syncStatus"));
    }

    @Test
    void unknownOperationFails() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("teleport", null, null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Unknown operation: teleport", result.errorMessage());
    }

    @Test
    void missingOperationFails() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor(null, "mymod", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Missing operation parameter", result.errorMessage());
    }

    @Test
    void fetchCompatibilityWithEmptyBodyFailsGracefully() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, ""), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("fetch_compatibility", "emptybody", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
    }

    @Test
    void getStatisticsWithEmptyBodyFailsGracefully() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, ""), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(contextFor("get_statistics", null, null)).block(TIMEOUT);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
    }

    @Test
    void submitResultWithEmptyBodyReturnsEmptyResponseField() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 202, ""), new AtomicInteger());
        engine.initialize().block(TIMEOUT);

        EngineResult result = engine.execute(
                contextFor("submit_result", "mymod", Map.of("k", "v"))).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals(true, data.get("success"));
        assertEquals(202, data.get("statusCode"));
        assertEquals("", data.get("response"));
    }

    @Test
    void shutdownResetsState() {
        NetworkSynchronizationEngine engine = engineWith(chain -> respond(chain, 200, "{}"), new AtomicInteger());
        engine.initialize().block(TIMEOUT);
        engine.shutdown().block(TIMEOUT);

        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());
        assertThrows(IllegalStateException.class,
                () -> engine.execute(contextFor("sync_cache", null, null)).block(TIMEOUT));
    }
}
