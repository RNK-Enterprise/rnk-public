package com.ld.thetync.engines;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Map;

/**
 * Sandboxing Engine - Provides isolated execution environments.
 * Uses Bouncy Castle and custom JVM sandboxing.
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
        // Create a restricted security manager
        SecurityManager originalManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new TyncSecurityManager());
            code.run();
            return "executed";
        } finally {
            System.setSecurityManager(originalManager);
        }
    }

    private static class TyncSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(java.security.Permission perm) {
            // Restrict dangerous permissions
            if (perm.getName().contains("Runtime") || perm.getName().contains("File")) {
                throw new SecurityException("Operation not allowed in sandbox");
            }
        }
    }
}