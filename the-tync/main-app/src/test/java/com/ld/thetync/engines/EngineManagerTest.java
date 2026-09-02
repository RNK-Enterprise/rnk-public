package com.ld.thetync.engines;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EngineManager}.
 */
class EngineManagerTest {

    /**
     * Test-local engine implementation with a controlled simple name.
     */
    private static class TestEngine implements Engine {
        @Override
        public String getName() {
            return "TestEngine";
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
        public reactor.core.publisher.Mono<EngineResult> execute(EngineContext context) {
            return reactor.core.publisher.Mono.just(EngineResult.success(null, 0L));
        }

        @Override
        public reactor.core.publisher.Mono<Void> shutdown() {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public EngineMetrics getMetrics() {
            return new EngineMetrics();
        }
    }

    private static final class OtherEngine extends TestEngine {
        @Override
        public String getName() {
            return "OtherEngine";
        }
    }

    @Test
    void registeredEngineIsRetrievableBySimpleName() {
        EngineManager manager = new EngineManager();
        TestEngine engine = new TestEngine();
        manager.registerEngine(engine);

        assertTrue(manager.getEngine("TestEngine").isPresent());
        assertEquals(engine, manager.getEngine("TestEngine").get());
        assertEquals(1, manager.getEngineCount());
    }

    @Test
    void unregisteredNameYieldsEmptyOptional() {
        EngineManager manager = new EngineManager();
        assertFalse(manager.getEngine("Nope").isPresent());
    }

    @Test
    void registerMultipleEnginesTracksCountAndNames() {
        EngineManager manager = new EngineManager();
        manager.registerEngine(new TestEngine());
        manager.registerEngine(new OtherEngine());

        assertEquals(2, manager.getEngineCount());
        Set<String> names = manager.getEngineNames();
        assertTrue(names.contains("TestEngine"));
        assertTrue(names.contains("OtherEngine"));

        Collection<Engine> all = manager.getAllEngines();
        assertEquals(2, all.size());
    }

    @Test
    void reRegisteringSameEngineTypeReplacesInstance() {
        EngineManager manager = new EngineManager();
        TestEngine first = new TestEngine();
        TestEngine second = new TestEngine();
        manager.registerEngine(first);
        manager.registerEngine(second);

        assertEquals(1, manager.getEngineCount());
        assertEquals(second, manager.getEngine("TestEngine").get());
    }

    @Test
    void newlyRegisteredEngineIsNotYetReportedHealthy() {
        EngineManager manager = new EngineManager();
        manager.registerEngine(new TestEngine());
        // EngineHealth defaults to UNKNOWN, which is not healthy.
        assertFalse(manager.isEngineHealthy("TestEngine"));
    }

    @Test
    void unknownEngineIsNotHealthy() {
        EngineManager manager = new EngineManager();
        assertFalse(manager.isEngineHealthy("Ghost"));
    }

    @Test
    void healthForUnknownEngineDefaultsToUnknownStatus() {
        EngineManager manager = new EngineManager();
        EngineHealth health = manager.getEngineHealth("Ghost");
        assertNotNull(health);
        assertEquals(EngineHealth.Status.UNKNOWN, health.getStatus());
    }

    @Test
    void healthForRegisteredEngineIsAvailable() {
        EngineManager manager = new EngineManager();
        manager.registerEngine(new TestEngine());
        EngineHealth health = manager.getEngineHealth("TestEngine");
        assertNotNull(health);
        assertEquals(EngineHealth.Status.UNKNOWN, health.getStatus());
    }
}
