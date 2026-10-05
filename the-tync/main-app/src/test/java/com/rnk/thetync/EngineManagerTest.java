package com.rnk.thetync;

import com.rnk.thetync.engines.Engine;
import com.rnk.thetync.engines.EngineContext;
import com.rnk.thetync.engines.EngineHealth;
import com.rnk.thetync.engines.EngineMetrics;
import com.rnk.thetync.engines.EngineResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EngineManager}, the name-keyed engine registry.
 */
class EngineManagerTest {

    private static class TestEngine implements Engine {
        private final String name;
        private final Mono<Void> shutdownResult;
        private int shutdownCalls;

        TestEngine(String name) {
            this(name, Mono.empty());
        }

        TestEngine(String name, Mono<Void> shutdownResult) {
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
        public Mono<EngineResult> execute(EngineContext context) {
            return Mono.just(EngineResult.success(null, 0L));
        }

        @Override
        public Mono<Void> shutdown() {
            shutdownCalls++;
            return shutdownResult;
        }

        @Override
        public EngineHealth getHealth() {
            return EngineHealth.HEALTHY;
        }

        @Override
        public EngineMetrics getMetrics() {
            return new EngineMetrics();
        }
    }

    @Test
    void registeredEngineIsRetrievableByName() {
        EngineManager manager = new EngineManager();
        TestEngine engine = new TestEngine("Alpha");
        manager.registerEngine(engine);

        assertSame(engine, manager.getEngine("Alpha"));
    }

    @Test
    void unknownEngineNameReturnsNull() {
        assertNull(new EngineManager().getEngine("Missing"));
    }

    @Test
    void registeringSameNameReplacesEngine() {
        EngineManager manager = new EngineManager();
        manager.registerEngine(new TestEngine("Alpha"));
        TestEngine replacement = new TestEngine("Alpha");
        manager.registerEngine(replacement);

        assertSame(replacement, manager.getEngine("Alpha"));
        assertEquals(1, manager.getAllEngines().size());
    }

    @Test
    void getAllEnginesReturnsDefensiveCopy() {
        EngineManager manager = new EngineManager();
        manager.registerEngine(new TestEngine("Alpha"));

        Map<String, Engine> snapshot = manager.getAllEngines();
        snapshot.clear();

        assertEquals(1, manager.getAllEngines().size());
    }

    @Test
    void shutdownAllShutsDownEveryEngineAndClearsRegistry() {
        EngineManager manager = new EngineManager();
        TestEngine alpha = new TestEngine("Alpha");
        TestEngine beta = new TestEngine("Beta");
        manager.registerEngine(alpha);
        manager.registerEngine(beta);

        manager.shutdownAll();

        assertEquals(1, alpha.shutdownCalls);
        assertEquals(1, beta.shutdownCalls);
        assertTrue(manager.getAllEngines().isEmpty());
    }

    @Test
    void shutdownAllContinuesPastFailingEngine() {
        EngineManager manager = new EngineManager();
        TestEngine failing = new TestEngine("Failing", Mono.error(new IllegalStateException("boom")));
        TestEngine healthy = new TestEngine("Healthy");
        manager.registerEngine(failing);
        manager.registerEngine(healthy);

        manager.shutdownAll();

        assertEquals(1, failing.shutdownCalls);
        assertEquals(1, healthy.shutdownCalls);
        assertTrue(manager.getAllEngines().isEmpty());
    }
}
