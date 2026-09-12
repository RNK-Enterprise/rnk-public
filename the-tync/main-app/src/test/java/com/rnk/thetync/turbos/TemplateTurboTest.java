package com.rnk.thetync.turbos;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Lifecycle contract tests for every template turbo. Each turbo shares the
 * same generated structure: uninitialized execute() rejects, initialize()
 * reports HEALTHY, execute() returns the turbo's standard result map,
 * shutdown() resets to UNKNOWN, and the acceleration factor matches the
 * value documented in the turbo's source.
 */
class TemplateTurboTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private record Spec(String name, double accelerationFactor) {
        Turbo turbo() {
            try {
                Class<?> clazz = Class.forName("com.rnk.thetync.turbos." + name);
                return (Turbo) clazz.getConstructor(io.micrometer.core.instrument.MeterRegistry.class)
                        .newInstance(new SimpleMeterRegistry());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot instantiate turbo " + name, e);
            }
        }
    }

    private static List<Spec> specs() {
        return List.of(
            new Spec("AccessorTurbo", 5.0),
            new Spec("AggregationTurbo", 6.0),
            new Spec("AllocationTurbo", 7.0),
            new Spec("AnalyticsTurbo", 8.0),
            new Spec("AnnouncementTurbo", 9.0),
            new Spec("ApiMappingTurbo", 8.0),
            new Spec("ApiTurbo", 6.0),
            new Spec("AssemblyTurbo", 10.0),
            new Spec("AssertionTurbo", 11.0),
            new Spec("AsyncTurbo", 12.0),
            new Spec("AuditTurbo", 13.0),
            new Spec("AuthenticationTurbo", 14.0),
            new Spec("AuthorizationTurbo", 15.0),
            new Spec("AutoTurbo", 4.0),
            new Spec("AvailabilityTurbo", 5.0),
            new Spec("BackupTurbo", 6.0),
            new Spec("BatchTurbo", 7.0),
            new Spec("BeautificationTurbo", 8.0),
            new Spec("BehaviorTurbo", 9.0),
            new Spec("BenchmarkTurbo", 10.0),
            new Spec("BindingTurbo", 11.0),
            new Spec("BiometricTurbo", 12.0),
            new Spec("BlacklistTurbo", 13.0),
            new Spec("BlenderTurbo", 14.0),
            new Spec("BlockingTurbo", 15.0),
            new Spec("BloomTurbo", 16.0),
            new Spec("BoostTurbo", 17.0),
            new Spec("BootstrapTurbo", 18.0),
            new Spec("BoundaryTurbo", 19.0),
            new Spec("BridgeTurbo", 7.0),
            new Spec("BroadcastTurbo", 20.0),
            new Spec("BrokerTurbo", 21.0),
            new Spec("BudgetTurbo", 22.0),
            new Spec("BuildTurbo", 23.0),
            new Spec("BulkTurbo", 24.0),
            new Spec("BundleTurbo", 25.0),
            new Spec("BurnTurbo", 12.0),
            new Spec("BusinessTurbo", 13.0),
            new Spec("ByteAlignmentTurbo", 14.0),
            new Spec("CacheTurbo", 15.0),
            new Spec("CacheWarmupTurbo", 15.0),
            new Spec("CallGraphTurbo", 16.0),
            new Spec("CanaryTurbo", 17.0),
            new Spec("CapacityTurbo", 18.0),
            new Spec("CaptchaTurbo", 19.0),
            new Spec("CatalogTurbo", 20.0),
            new Spec("CatchTurbo", 21.0),
            new Spec("CertificationTurbo", 22.0),
            new Spec("ChainTurbo", 23.0),
            new Spec("ChannelTurbo", 24.0),
            new Spec("CheckpointTurbo", 25.0),
            new Spec("ClassLoaderTurbo", 26.0),
            new Spec("ClassificationTurbo", 27.0),
            new Spec("CleanupTurbo", 28.0),
            new Spec("CloningTurbo", 29.0),
            new Spec("CloudTurbo", 30.0),
            new Spec("ClusterTurbo", 31.0),
            new Spec("CoalesceTurbo", 32.0),
            new Spec("CodegenTurbo", 33.0),
            new Spec("CohesionTurbo", 34.0),
            new Spec("CollaborationTurbo", 35.0),
            new Spec("CollectorTurbo", 36.0),
            new Spec("ColorTurbo", 37.0),
            new Spec("ColumnTurbo", 38.0),
            new Spec("CommentTurbo", 39.0),
            new Spec("CommitTurbo", 40.0),
            new Spec("CommunicationTurbo", 5.0),
            new Spec("CompactionTurbo", 5.0),
            new Spec("ComparatorTurbo", 6.0),
            new Spec("ComparisonTurbo", 7.0),
            new Spec("CompeterTurbo", 8.0),
            new Spec("CompilationTurbo", 9.0),
            new Spec("ConstraintTurbo", 10.0),
            new Spec("ContainerTurbo", 11.0),
            new Spec("ContextTurbo", 12.0),
            new Spec("IssueResolutionTurbo", 10.0),
            new Spec("LearningTurbo", 12.0),
            new Spec("MetamorphosisTurbo", 5.0),
            new Spec("MetricsTurbo", 3.0),
            new Spec("MultiLoaderTurbo", 16.0),
            new Spec("NlpTurbo", 8.0),
            new Spec("OptimizationTurbo", 15.0),
            new Spec("OrchestratorTurbo", 12.0),
            new Spec("PatternTurbo", 11.0),
            new Spec("PredictionTurbo", 11.0),
            new Spec("ReactiveTurbo", 13.0),
            new Spec("SandboxTurbo", 6.0),
            new Spec("SecurityTurbo", 9.0),
            new Spec("SerializationTurbo", 7.0),
            new Spec("SyncTurbo", 7.0),
            new Spec("ValidationTurbo", 4.0)
        );
    }

    @TestFactory
    Stream<DynamicTest> fullLifecyclePerTurbo() {
        return specs().stream().map(spec -> dynamicTest(spec.name(), () -> {
            Turbo turbo = spec.turbo();

            assertEquals(spec.name(), turbo.getName());
            assertEquals("1.0.0", turbo.getVersion());
            assertThrows(IllegalStateException.class,
                    () -> turbo.execute(new TurboContext()).block(TIMEOUT));

            turbo.initialize().block(TIMEOUT);
            assertEquals(TurboHealth.HEALTHY, turbo.getHealth());
            assertTrue(turbo.getMetrics() == turbo.getMetrics());

            TurboResult result = turbo.execute(new TurboContext()).block(TIMEOUT);
            assertTrue(result.isSuccess(), spec.name() + " execute should succeed");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertEquals("executed", data.get("status"));
            assertEquals(spec.name(), data.get("turbo"));
            assertEquals(spec.accelerationFactor(), turbo.getAccelerationFactor());
            assertEquals(spec.accelerationFactor(), result.getAccelerationFactor());
            assertTrue(result.getExecutionTimeMs() >= 0);
            assertEquals(null, result.getErrorMessage());

            turbo.shutdown().block(TIMEOUT);
            assertEquals(TurboHealth.UNKNOWN, turbo.getHealth());
            assertThrows(IllegalStateException.class,
                    () -> turbo.execute(new TurboContext()).block(TIMEOUT));
        }));
    }

    @TestFactory
    Stream<DynamicTest> accelerationFactorsArePositive() {
        return specs().stream().map(spec -> dynamicTest(spec.name() + "#acceleration", () -> {
            Turbo turbo = spec.turbo();
            assertTrue(spec.accelerationFactor() > 0, spec.name() + " must accelerate");
            assertEquals(spec.accelerationFactor(), turbo.getAccelerationFactor());
        }));
    }
}
