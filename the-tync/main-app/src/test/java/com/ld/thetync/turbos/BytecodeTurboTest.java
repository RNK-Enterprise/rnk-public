package com.ld.thetync.turbos;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BytecodeTurbo}: lifecycle, predictive-cache miss and hit
 * paths, and shutdown cache invalidation.
 */
class BytecodeTurboTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private BytecodeTurbo newTurbo() {
        return new BytecodeTurbo(new SimpleMeterRegistry());
    }

    private TurboContext contextFor(String cacheKey, byte[] bytecode) {
        TurboContext context = new TurboContext();
        context.put("cacheKey", cacheKey);
        context.put("bytecode", bytecode);
        return context;
    }

    @Test
    void executeBeforeInitializeFails() {
        BytecodeTurbo turbo = newTurbo();
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("k", new byte[]{1})).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        assertEquals(TurboHealth.HEALTHY, turbo.getHealth());
        assertEquals("BytecodeTurbo", turbo.getName());
        assertEquals("1.0.0", turbo.getVersion());
        assertTrue(turbo.getMetrics() == turbo.getMetrics());
        assertTrue(turbo.getAccelerationFactor() > 0);
    }

    @Test
    void firstExecuteTransformsBytecode() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        byte[] original = {1, 2, 3, 4};

        TurboResult result = turbo.execute(contextFor("key1", original)).block(TIMEOUT);

        assertTrue(result.isSuccess());
        assertTrue(result.getData() instanceof byte[]);
        byte[] transformed = (byte[]) result.getData();
        assertEquals(original.length, transformed.length);
        assertEquals(original[0], transformed[0]);
        assertEquals(turbo.getAccelerationFactor(), result.getAccelerationFactor());
    }

    @Test
    void repeatedKeyServesCacheHitWithoutTransform() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        byte[] original = {9, 8, 7};

        TurboResult first = turbo.execute(contextFor("hit-key", original)).block(TIMEOUT);
        TurboResult second = turbo.execute(contextFor("hit-key", original)).block(TIMEOUT);

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        byte[] firstData = (byte[]) first.getData();
        byte[] secondData = (byte[]) second.getData();
        assertEquals(firstData.length, secondData.length);
        for (int i = 0; i < firstData.length; i++) {
            assertEquals(firstData[i], secondData[i]);
        }
    }

    @Test
    void distinctKeysTransformIndependently() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        TurboResult a = turbo.execute(contextFor("ka", new byte[]{1})).block(TIMEOUT);
        TurboResult b = turbo.execute(contextFor("kb", new byte[]{2})).block(TIMEOUT);

        assertTrue(a.isSuccess());
        assertTrue(b.isSuccess());
        assertEquals(1, ((byte[]) a.getData())[0]);
        assertEquals(2, ((byte[]) b.getData())[0]);
    }

    @Test
    void shutdownResetsState() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        turbo.shutdown().block(TIMEOUT);

        assertEquals(TurboHealth.UNKNOWN, turbo.getHealth());
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("k", new byte[]{1})).block(TIMEOUT));
    }

    @Test
    void successResultCarriesNoError() {
        BytecodeTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        TurboResult result = turbo.execute(contextFor("k", new byte[]{1})).block(TIMEOUT);

        assertTrue(result.isSuccess());
        assertEquals(null, result.getErrorMessage());
        assertTrue(result.getExecutionTimeMs() >= 0);
    }
}
