package com.ld.thetync.turbos;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ShimTurbo}: lifecycle, shim generation content, and
 * shutdown.
 */
class ShimTurboTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ShimTurbo newTurbo() {
        return new ShimTurbo(new SimpleMeterRegistry());
    }

    private TurboContext contextFor(String sourceApi, String targetApi) {
        TurboContext context = new TurboContext();
        context.put("sourceApi", sourceApi);
        context.put("targetApi", targetApi);
        return context;
    }

    @Test
    void executeBeforeInitializeFails() {
        ShimTurbo turbo = newTurbo();
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("a", "b")).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        ShimTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        assertEquals(TurboHealth.HEALTHY, turbo.getHealth());
        assertEquals("ShimTurbo", turbo.getName());
        assertEquals("1.0.0", turbo.getVersion());
        assertTrue(turbo.getMetrics() == turbo.getMetrics());
        assertEquals(9.0, turbo.getAccelerationFactor());
    }

    @Test
    void executeGeneratesShimReferencingBothApis() {
        ShimTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        TurboResult result = turbo.execute(contextFor("OldApi", "NewApi")).block(TIMEOUT);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.getData();
        String shimCode = (String) data.get("shimCode");
        assertTrue(shimCode.contains("OldApi"));
        assertTrue(shimCode.contains("NewApi"));
        assertTrue(shimCode.contains("adapt"));
        assertEquals(turbo.getAccelerationFactor(), result.getAccelerationFactor());
    }

    @Test
    void executeHandlesMissingApiInputs() {
        ShimTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        // Missing keys read back as null, exercising the generator's null path.
        TurboResult result = turbo.execute(new TurboContext()).block(TIMEOUT);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.getData();
        String shimCode = (String) data.get("shimCode");
        assertTrue(shimCode.contains("null"));
    }

    @Test
    void shutdownResetsState() {
        ShimTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        turbo.shutdown().block(TIMEOUT);

        assertEquals(TurboHealth.UNKNOWN, turbo.getHealth());
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("a", "b")).block(TIMEOUT));
    }
}
