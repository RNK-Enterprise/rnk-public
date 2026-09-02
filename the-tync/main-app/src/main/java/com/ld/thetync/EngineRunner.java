package com.ld.thetync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ld.thetync.engines.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Engine Runner - Command line interface for executing engines from Node.js
 */
public class EngineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EngineRunner.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: EngineRunner <engineName> <contextJson>");
            System.exit(1);
        }

        String engineName = args[0];
        String contextJson = args[1];

        try {
            // Parse context
            @SuppressWarnings("unchecked")
            Map<String, Object> context = objectMapper.readValue(contextJson, Map.class);

            // Execute engine
            EngineResult result = executeEngine(engineName, context);

            // Output result as JSON
            String resultJson = objectMapper.writeValueAsString(result);
            System.out.println(resultJson);

        } catch (Exception e) {
            logger.error("Engine execution failed", e);
            try {
                EngineResult errorResult = EngineResult.failure(e.getMessage(), 0);
                String errorJson = objectMapper.writeValueAsString(errorResult);
                System.out.println(errorJson);
            } catch (Exception jsonError) {
                System.err.println("{\"success\":false,\"errorMessage\":\"" + e.getMessage() + "\",\"executionTimeMs\":0}");
            }
            System.exit(1);
        }
    }

    private static EngineResult executeEngine(String engineName, Map<String, Object> context) throws Exception {
        // Create meter registry (simple implementation for CLI)
        io.micrometer.core.instrument.MeterRegistry meterRegistry =
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        // Create engine instance based on name
        Engine engine = createEngine(engineName, meterRegistry);

        if (engine == null) {
            throw new IllegalArgumentException("Unknown engine: " + engineName);
        }

        // Initialize engine if needed
        if (engine.getHealth() == EngineHealth.UNKNOWN) {
            engine.initialize().block();
        }

        // Execute engine
        EngineContext engineContext = new EngineContext(context);
        CompletableFuture<EngineResult> future = new CompletableFuture<>();

        engine.execute(engineContext)
            .doOnSuccess(future::complete)
            .doOnError(throwable -> future.completeExceptionally(throwable))
            .subscribe();

        return future.get(); // Wait for completion
    }

    private static Engine createEngine(String engineName, io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        switch (engineName) {
            case "AdaptiveLearningEngine":
                return new AdaptiveLearningEngine(meterRegistry);
            case "PredictiveOptimizationEngine":
                return new PredictiveOptimizationEngine(meterRegistry);
            // Add more engines as needed
            default:
                logger.warn("Engine not implemented yet: {}", engineName);
                return null;
        }
    }
}