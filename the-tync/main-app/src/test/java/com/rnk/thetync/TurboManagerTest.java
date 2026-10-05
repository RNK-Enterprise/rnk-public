package com.rnk.thetync;

import com.rnk.thetync.turbos.Turbo;
import com.rnk.thetync.turbos.TurboContext;
import com.rnk.thetync.turbos.TurboHealth;
import com.rnk.thetync.turbos.TurboMetrics;
import com.rnk.thetync.turbos.TurboResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TurboManager}, the name-keyed turbo registry.
 */
class TurboManagerTest {

    private static class TestTurbo implements Turbo {
        private final String name;
        private final Mono<Void> shutdownResult;
        private int shutdownCalls;

        TestTurbo(String name) {
            this(name, Mono.empty());
        }

        TestTurbo(String name, Mono<Void> shutdownResult) {
            this.name = name;
            this.shutdownResult = shutdownResult;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return "1.0.0-test";
        }

        @Override
        public Mono<Void> initialize() {
            return Mono.empty();
        }

        @Override
        public Mono<TurboResult> execute(TurboContext context) {
            return Mono.just(TurboResult.success(null, 0L, 1.0));
        }

        @Override
        public Mono<Void> shutdown() {
            shutdownCalls++;
            return shutdownResult;
        }

        @Override
        public TurboHealth getHealth() {
            return TurboHealth.HEALTHY;
        }

        @Override
        public TurboMetrics getMetrics() {
            return new TurboMetrics();
        }

        @Override
        public double getAccelerationFactor() {
            return 1.0;
        }
    }

    @Test
    void registeredTurboIsRetrievableByName() {
        TurboManager manager = new TurboManager();
        TestTurbo turbo = new TestTurbo("Alpha");
        manager.registerTurbo(turbo);

        assertSame(turbo, manager.getTurbo("Alpha"));
    }

    @Test
    void unknownTurboNameReturnsNull() {
        assertNull(new TurboManager().getTurbo("Missing"));
    }

    @Test
    void registeringSameNameReplacesTurbo() {
        TurboManager manager = new TurboManager();
        manager.registerTurbo(new TestTurbo("Alpha"));
        TestTurbo replacement = new TestTurbo("Alpha");
        manager.registerTurbo(replacement);

        assertSame(replacement, manager.getTurbo("Alpha"));
        assertEquals(1, manager.getAllTurbos().size());
    }

    @Test
    void getAllTurbosReturnsDefensiveCopy() {
        TurboManager manager = new TurboManager();
        manager.registerTurbo(new TestTurbo("Alpha"));

        Map<String, Turbo> snapshot = manager.getAllTurbos();
        snapshot.clear();

        assertEquals(1, manager.getAllTurbos().size());
    }

    @Test
    void shutdownAllShutsDownEveryTurboAndClearsRegistry() {
        TurboManager manager = new TurboManager();
        TestTurbo alpha = new TestTurbo("Alpha");
        TestTurbo beta = new TestTurbo("Beta");
        manager.registerTurbo(alpha);
        manager.registerTurbo(beta);

        manager.shutdownAll();

        assertEquals(1, alpha.shutdownCalls);
        assertEquals(1, beta.shutdownCalls);
        assertTrue(manager.getAllTurbos().isEmpty());
    }

    @Test
    void shutdownAllContinuesPastFailingTurbo() {
        TurboManager manager = new TurboManager();
        TestTurbo failing = new TestTurbo("Failing", Mono.error(new IllegalStateException("boom")));
        TestTurbo healthy = new TestTurbo("Healthy");
        manager.registerTurbo(failing);
        manager.registerTurbo(healthy);

        manager.shutdownAll();

        assertEquals(1, failing.shutdownCalls);
        assertEquals(1, healthy.shutdownCalls);
        assertTrue(manager.getAllTurbos().isEmpty());
    }
}
