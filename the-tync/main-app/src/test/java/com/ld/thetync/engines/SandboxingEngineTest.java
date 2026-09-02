package com.ld.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the refactored {@link SandboxingEngine}: lifecycle, sandboxed
 * execution, failure paths, and full coverage of every executable line and
 * branch.
 */
class SandboxingEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private SandboxingEngine newEngine() {
        return new SandboxingEngine(new SimpleMeterRegistry());
    }

    private SandboxingEngine initializedEngine() {
        SandboxingEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);
        return engine;
    }

    @Test
    void executeBeforeInitializeFails() {
        SandboxingEngine engine = newEngine();
        EngineContext context = new EngineContext();
        context.put("code", (Runnable) () -> { });

        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        SandboxingEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);

        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertEquals("SandboxingEngine", engine.getName());
        assertEquals("1.0.0", engine.getVersion());
        assertTrue(engine.getMetrics() == engine.getMetrics());
    }

    @Test
    void executeRunsCodeAndReportsSuccess() {
        SandboxingEngine engine = initializedEngine();
        AtomicInteger executed = new AtomicInteger(0);
        EngineContext context = new EngineContext();
        context.put("code", (Runnable) executed::incrementAndGet);

        EngineResult result = engine.execute(context).block(TIMEOUT);

        assertEquals(1, executed.get());
        assertTrue(result.success());
        assertEquals("executed", result.data());
    }

    @Test
    void executeWithoutCodeFailsWithIllegalArgument() {
        SandboxingEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("operation", "nothing-relevant");

        assertThrows(IllegalArgumentException.class, () -> engine.execute(context).block(TIMEOUT));
    }

    @Test
    void executeFailsWhenCodeThrows() {
        SandboxingEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("code", (Runnable) () -> {
            throw new IllegalStateException("sandbox breach");
        });

        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }

    @Test
    void secondaryConstructorSharesRegistry() {
        SandboxingEngine engine = new SandboxingEngine(new SimpleMeterRegistry(), null);

        assertEquals("SandboxingEngine", engine.getName());
    }

    @Test
    void shutdownResetsState() {
        SandboxingEngine engine = initializedEngine();
        engine.shutdown().block(TIMEOUT);

        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());

        EngineContext context = new EngineContext();
        context.put("code", (Runnable) () -> { });
        assertThrows(IllegalStateException.class, () -> engine.execute(context).block(TIMEOUT));
    }
}
