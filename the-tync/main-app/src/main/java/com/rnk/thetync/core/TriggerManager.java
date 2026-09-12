package com.rnk.thetync.core;

import com.rnk.thetync.engines.Engine;
import com.rnk.thetync.engines.EngineContext;
import com.rnk.thetync.engines.EngineResult;
import com.rnk.thetync.turbos.Turbo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Trigger Manager - Implements trigger-based firing for engines and turbos
 * Enables event-driven activation based on conditions, events, and mod requirements
 */
public class TriggerManager {
    private static final Logger logger = LoggerFactory.getLogger(TriggerManager.class);

    private final MeterRegistry meterRegistry;
    private final ComponentLoader componentLoader;
    private final Map<String, List<TriggerRule>> triggerRules = new ConcurrentHashMap<>();
    private final Counter triggerActivations;

    public TriggerManager(MeterRegistry meterRegistry, ComponentLoader componentLoader) {
        this.meterRegistry = meterRegistry;
        this.componentLoader = componentLoader;
        this.triggerActivations = Counter.builder("triggers.activated")
                .description("Number of trigger activations")
                .register(meterRegistry);
    }

    /**
     * Initialize the trigger manager
     */
    public void initialize() {
        logger.info("TriggerManager initialized");
    }

    /**
     * Register a trigger rule for an engine
     */
    public void registerEngineTrigger(String engineName, TriggerRule rule) {
        triggerRules.computeIfAbsent("engine:" + engineName, k -> new CopyOnWriteArrayList<>()).add(rule);
        logger.debug("Registered trigger rule for engine: {}", engineName);
    }

    /**
     * Register a trigger rule for a turbo
     */
    public void registerTurboTrigger(String turboName, TriggerRule rule) {
        triggerRules.computeIfAbsent("turbo:" + turboName, k -> new CopyOnWriteArrayList<>()).add(rule);
        logger.debug("Registered trigger rule for turbo: {}", turboName);
    }

    /**
     * Evaluate triggers and activate components based on context
     */
    public Mono<Void> evaluateTriggers(TriggerContext context) {
        logger.debug("Evaluating triggers for context: {}", context.getType());

        return Mono.fromRunnable(() -> {
            triggerRules.forEach((componentKey, rules) -> {
                for (TriggerRule rule : rules) {
                    if (rule.evaluate(context)) {
                        activateComponent(componentKey, context).subscribe();
                        triggerActivations.increment();
                    }
                }
            });
        });
    }

    private Mono<Void> activateComponent(String componentKey, TriggerContext context) {
        String[] parts = componentKey.split(":");
        if (parts.length != 2) return Mono.empty();

        String type = parts[0];
        String name = parts[1];

        try {
            if ("engine".equals(type)) {
                return componentLoader.getEngine(name)
                    .flatMap(engine -> initializeEngine(engine, context));
            } else if ("turbo".equals(type)) {
                return componentLoader.getTurbo(name)
                    .flatMap(turbo -> initializeTurbo(turbo, context));
            }
        } catch (Exception e) {
            logger.warn("Failed to activate component: {}", componentKey, e);
        }
        return Mono.empty();
    }

    private Mono<Void> initializeEngine(Engine engine, TriggerContext context) {
        EngineContext engineContext = new EngineContext();
        engineContext.put("triggerType", context.getType());
        engineContext.put("triggerData", context.getData());

        return engine.execute(engineContext).then();
    }

    private Mono<Void> initializeTurbo(Turbo turbo, TriggerContext context) {
        // Turbos don't have execute method in the same way, just initialize
        return turbo.initialize();
    }

    /**
     * Trigger rule definition
     */
    public static class TriggerRule {
        private final String name;
        private final Predicate<TriggerContext> condition;
        private final String description;

        public TriggerRule(String name, Predicate<TriggerContext> condition, String description) {
            this.name = name;
            this.condition = condition;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }

        public boolean evaluate(TriggerContext context) {
            return condition.test(context);
        }
    }

    /**
     * Trigger context containing event data
     */
    public static class TriggerContext {
        private final String type;
        private final Map<String, Object> data;
        private final long timestamp;

        public TriggerContext(String type, Map<String, Object> data) {
            this.type = type;
            this.data = Map.copyOf(data);
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }

        public Object get(String key) {
            return data.get(key);
        }

        // Common trigger context factories
        public static TriggerContext modLoading(String modId, int modCount) {
            return new TriggerContext("mod-loading",
                    Map.of("modId", modId, "modCount", modCount));
        }

        public static TriggerContext performanceThreshold(String metric, double value) {
            return new TriggerContext("performance-threshold",
                    Map.of("metric", metric, "value", value));
        }

        public static TriggerContext minecraftEvent(String eventType, Object eventData) {
            return new TriggerContext("minecraft-event",
                    Map.of("eventType", eventType, "eventData", eventData));
        }
    }
}