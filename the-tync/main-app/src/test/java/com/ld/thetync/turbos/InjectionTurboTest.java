package com.ld.thetync.turbos;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InjectionTurbo}: lifecycle, pass-through injection
 * semantics, and shutdown.
 */
class InjectionTurboTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private InjectionTurbo newTurbo() {
        return new InjectionTurbo(new SimpleMeterRegistry());
    }

    private TurboContext contextFor(String className, byte[] bytecode) {
        TurboContext context = new TurboContext();
        context.put("className", className);
        context.put("bytecode", bytecode);
        return context;
    }

    @Test
    void executeBeforeInitializeFails() {
        InjectionTurbo turbo = newTurbo();
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("C", new byte[]{1})).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        InjectionTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        assertEquals(TurboHealth.HEALTHY, turbo.getHealth());
        assertEquals("InjectionTurbo", turbo.getName());
        assertEquals("1.0.0", turbo.getVersion());
        assertTrue(turbo.getMetrics() == turbo.getMetrics());
        assertEquals(14.0, turbo.getAccelerationFactor());
    }

    @Test
    void executeReturnsInjectedBytecode() {
        InjectionTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        byte[] original = {5, 6, 7};

        TurboResult result = turbo.execute(contextFor("com.example.Target", original)).block(TIMEOUT);

        assertTrue(result.isSuccess());
        assertArrayEquals(original, (byte[]) result.getData());
        assertEquals(turbo.getAccelerationFactor(), result.getAccelerationFactor());
    }

    @Test
    void executeHandlesMissingClassNameAndBytecode() {
        InjectionTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);

        // Missing keys read back as null, exercising the injection null path.
        TurboResult result = turbo.execute(new TurboContext()).block(TIMEOUT);

        assertTrue(result.isSuccess());
        assertEquals(null, result.getData());
    }

    @Test
    void shutdownResetsState() {
        InjectionTurbo turbo = newTurbo();
        turbo.initialize().block(TIMEOUT);
        turbo.shutdown().block(TIMEOUT);

        assertEquals(TurboHealth.UNKNOWN, turbo.getHealth());
        assertThrows(IllegalStateException.class,
                () -> turbo.execute(contextFor("C", new byte[]{1})).block(TIMEOUT));
    }
}
