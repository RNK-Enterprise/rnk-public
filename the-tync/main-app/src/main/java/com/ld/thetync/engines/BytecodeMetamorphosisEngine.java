package com.ld.thetync.engines;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.instrument.Instrumentation;

/**
 * Proprietary component of The Tync. The implementation is not part of the
 * open-source distribution; this placeholder keeps the public framework
 * compiling and runnable.
 *
 * <p>Contract: receives the shared {@link EngineContext}, performs real-time
 * bytecode transformation, and returns an {@link EngineResult}.</p>
 */
public class BytecodeMetamorphosisEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(BytecodeMetamorphosisEngine.class);

    private final EngineMetrics metrics = new EngineMetrics();

    @SuppressWarnings("unused")
    private final MeterRegistry meterRegistry;

    @SuppressWarnings("unused")
    private final Instrumentation instrumentation;

    public BytecodeMetamorphosisEngine(MeterRegistry meterRegistry, Instrumentation instrumentation) {
        this.meterRegistry = meterRegistry;
        this.instrumentation = instrumentation;
    }

    @Override
    public String getName() {
        return "BytecodeMetamorphosisEngine";
    }

    @Override
    public String getVersion() {
        return "1.0.0-stub";
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() ->
            logger.warn("{} is a proprietary stub - real implementation not included in this distribution", getName()));
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        long start = System.currentTimeMillis();
        metrics.recordExecution(false, 0);
        return Mono.just(EngineResult.failure(
            getName() + " is proprietary and not included in the open-source distribution",
            System.currentTimeMillis() - start));
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.empty();
    }

    @Override
    public EngineMetrics getMetrics() {
        return metrics;
    }
}
