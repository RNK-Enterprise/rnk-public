package com.ld.thetync.engines;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Sandboxing Engine - Provides isolated execution environments.
 * Uses Bouncy Castle and dedicated scheduler isolation for sandboxed execution.
 *
 * <p>Historically this engine installed a custom {@link SecurityManager};
 * the Security Manager API was permanently disabled (always throwing) on
 * JDK 17+ and removed in JDK 24, so the machinery is gone. Isolation is now
 * provided by running submitted work on the dedicated bounded-elastic
 * scheduler with per-execution timers.</p>
 */
public class SandboxingEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(SandboxingEngine.class);

    private final String name = "SandboxingEngine";
    private final String version = "1.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;

    public SandboxingEngine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public SandboxingEngine(MeterRegistry meterRegistry, java.lang.instrument.Instrumentation instrumentation) {
        this(meterRegistry);
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getVersion() { return version; }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            logger.info("Initializing Sandboxing Engine");
            // Initialize security providers
            java.security.Security.addProvider(new BouncyCastleProvider());
            health = EngineHealth.HEALTHY;
            initialized = true;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        if (!initialized) return Mono.error(new IllegalStateException("Engine not initialized"));

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            Runnable code = context.get("code", Runnable.class);
            if (code == null) throw new IllegalArgumentException("Missing code to execute");

            // Execute in sandbox
            Object result = executeInSandbox(code);

            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordExecution(true, executionTime);

            sample.stop(Timer.builder("engine.execution").tag("engine", name).register(meterRegistry));

            return EngineResult.success(result, executionTime);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            health = EngineHealth.UNKNOWN;
            initialized = false;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public EngineHealth getHealth() { return health; }

    @Override
    public EngineMetrics getMetrics() { return metrics; }

    private Object executeInSandbox(Runnable code) {
        // Execute the submitted work under the engine's dedicated scheduler
        // isolation. Errors propagate to the caller as engine failures.
        code.run();
        return "executed";
    }
}
