package com.rnk.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

import java.lang.instrument.Instrumentation;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the eleven proprietary-engine stubs. Every stub must expose
 * the same contract: correct name, stub version, warn-only initialization,
 * an unsupported {@link EngineResult} on execute, clean shutdown, and live
 * metrics.
 */
class StubEnginesTest {

    /**
     * No-op instrumentation double; stubs never invoke it.
     */
    private static final Instrumentation INSTRUMENTATION = mock(Instrumentation.class);

    /**
     * Factory record pairing a stub with its expected engine name.
     */
    private record StubFactory(String expectedName, Function<SimpleMeterRegistry, Engine> factory) {
    }

    private static java.util.List<StubFactory> stubFactories() {
        SimpleMeterRegistry ignored = new SimpleMeterRegistry();
        return java.util.List.of(
            new StubFactory("BytecodeMetamorphosisEngine",
                mr -> new BytecodeMetamorphosisEngine(mr, INSTRUMENTATION)),
            new StubFactory("InjectionEngine",
                mr -> new InjectionEngine(mr, INSTRUMENTATION)),
            new StubFactory("ShimGenerationEngine", ShimGenerationEngine::new),
            new StubFactory("ValidationEngine", ValidationEngine::new),
            new StubFactory("PatternRecognitionEngine", PatternRecognitionEngine::new),
            new StubFactory("AdaptiveLearningEngine", AdaptiveLearningEngine::new),
            new StubFactory("PredictiveOptimizationEngine", PredictiveOptimizationEngine::new),
            new StubFactory("ApiMappingEngine", ApiMappingEngine::new),
            new StubFactory("ApiBridgeEngine", ApiBridgeEngine::new),
            new StubFactory("IssueResolutionEngine", IssueResolutionEngine::new),
            new StubFactory("MultiLoaderBridgeEngine", MultiLoaderBridgeEngine::new));
    }

    @Test
    void allStubEnginesExposeExpectedNames() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            Engine engine = stub.factory().apply(registry);
            assertEquals(stub.expectedName(), engine.getName(),
                () -> "unexpected name for " + stub.expectedName());
        }
    }

    @Test
    void allStubEnginesReportStubVersion() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            assertEquals("1.0.0-stub", stub.factory().apply(registry).getVersion());
        }
    }

    @Test
    void allStubEnginesInitializeWithoutError() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            Engine engine = stub.factory().apply(registry);
            engine.initialize().block();
            // Engine.getHealth() defaults to a HEALTHY status instance.
            assertEquals(EngineHealth.Status.HEALTHY, engine.getHealth().getStatus());
        }
    }

    @Test
    void allStubEnginesReturnUnsupportedResultAndRecordFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            Engine engine = stub.factory().apply(registry);
            EngineContext context = new EngineContext();
            context.put("input", "test.jar");

            EngineResult result = engine.execute(context).block();
            assertNotNull(result, stub.expectedName());
            assertFalse(result.success());
            assertNull(result.data());
            assertTrue(result.errorMessage().contains("proprietary"),
                () -> stub.expectedName() + " message should mention proprietary: "
                    + result.errorMessage());
            assertTrue(result.executionTimeMs() >= 0);

            assertEquals(1, engine.getMetrics().getTotalExecutions());
            assertEquals(0, engine.getMetrics().getSuccessfulExecutions());
            assertEquals(1, engine.getMetrics().getFailedExecutions());
        }
    }

    @Test
    void allStubEnginesShutDownCleanly() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            Engine engine = stub.factory().apply(registry);
            Mono<Void> shutdown = engine.shutdown();
            assertNotNull(shutdown);
            shutdown.block();
        }
    }

    @Test
    void allStubEnginesProvideMetricsInstance() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (StubFactory stub : stubFactories()) {
            assertNotNull(stub.factory().apply(registry).getMetrics());
        }
    }

    @Test
    void stubConstructorAcceptsLiveInstrumentationReference() {
        // Both instrumentation-backed stubs must compile-link against the
        // two-argument constructor shape used by TheTync registration.
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Engine metamorphosis = new BytecodeMetamorphosisEngine(registry, INSTRUMENTATION);
        Engine injection = new InjectionEngine(registry, INSTRUMENTATION);
        assertNotNull(metamorphosis);
        assertNotNull(injection);
    }
}
