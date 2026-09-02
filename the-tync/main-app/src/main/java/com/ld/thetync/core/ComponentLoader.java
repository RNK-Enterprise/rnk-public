package com.ld.thetync.core;

import com.ld.thetync.engines.Engine;
import com.ld.thetync.engines.EngineManager;
import com.ld.thetync.turbos.Turbo;
import com.ld.thetync.turbos.TurboManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Component Loader - Implements lazy loading for engines and turbos
 * Loads components on-demand to reduce startup time and memory footprint
 */
public class ComponentLoader {
    private static final Logger logger = LoggerFactory.getLogger(ComponentLoader.class);

    private final Map<String, LazyEngine> lazyEngines = new ConcurrentHashMap<>();
    private final Map<String, LazyTurbo> lazyTurbos = new ConcurrentHashMap<>();
    private final EngineManager engineManager;
    private final TurboManager turboManager;

    public ComponentLoader(EngineManager engineManager, TurboManager turboManager) {
        this.engineManager = engineManager;
        this.turboManager = turboManager;
    }

    /**
     * Register an engine for lazy loading
     */
    public void registerLazyEngine(String name, Supplier<Engine> engineSupplier) {
        lazyEngines.put(name, new LazyEngine(name, engineSupplier));
        logger.debug("Registered lazy engine: {}", name);
    }

    /**
     * Register a turbo for lazy loading
     */
    public void registerLazyTurbo(String name, Supplier<Turbo> turboSupplier) {
        lazyTurbos.put(name, new LazyTurbo(name, turboSupplier));
        logger.debug("Registered lazy turbo: {}", name);
    }

    /**
     * Get an engine, loading it lazily if not already loaded
     */
    public Mono<Engine> getEngine(String name) {
        LazyEngine lazyEngine = lazyEngines.get(name);
        if (lazyEngine == null) {
            return Mono.error(new IllegalArgumentException("Engine not registered: " + name));
        }

        if (lazyEngine.isLoaded()) {
            return Mono.just(lazyEngine.getEngine());
        }

        return Mono.fromCallable(() -> {
            logger.debug("Lazy loading engine: {}", name);
            Engine engine = lazyEngine.load();
            engineManager.registerEngine(engine);
            return engine;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get a turbo, loading it lazily if not already loaded
     */
    public Mono<Turbo> getTurbo(String name) {
        LazyTurbo lazyTurbo = lazyTurbos.get(name);
        if (lazyTurbo == null) {
            return Mono.error(new IllegalArgumentException("Turbo not registered: " + name));
        }

        if (lazyTurbo.isLoaded()) {
            return Mono.just(lazyTurbo.getTurbo());
        }

        return Mono.fromCallable(() -> {
            logger.debug("Lazy loading turbo: {}", name);
            Turbo turbo = lazyTurbo.load();
            turboManager.registerTurbo(turbo);
            return turbo;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Preload critical components for immediate availability
     */
    public Mono<Void> preloadCriticalComponents() {
        return Mono.fromCallable(() -> {
            logger.info("Preloading critical components");

            // Define critical components that should be loaded immediately
            String[] criticalEngines = {"BytecodeMetamorphosisEngine", "ValidationEngine"};
            String[] criticalTurbos = {"BytecodeTurbo", "CacheTurbo"};

            // Load critical engines
            for (String engineName : criticalEngines) {
                LazyEngine lazyEngine = lazyEngines.get(engineName);
                if (lazyEngine != null && !lazyEngine.isLoaded()) {
                    try {
                        Engine engine = lazyEngine.load();
                        engineManager.registerEngine(engine);
                        logger.debug("Preloaded critical engine: {}", engineName);
                    } catch (Exception e) {
                        logger.warn("Failed to preload critical engine: {}", engineName, e);
                    }
                }
            }

            // Load critical turbos
            for (String turboName : criticalTurbos) {
                LazyTurbo lazyTurbo = lazyTurbos.get(turboName);
                if (lazyTurbo != null && !lazyTurbo.isLoaded()) {
                    try {
                        Turbo turbo = lazyTurbo.load();
                        turboManager.registerTurbo(turbo);
                        logger.debug("Preloaded critical turbo: {}", turboName);
                    } catch (Exception e) {
                        logger.warn("Failed to preload critical turbo: {}", turboName, e);
                    }
                }
            }

            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Initialize the component loader
     */
    public void initialize() {
        logger.info("ComponentLoader initialized");
    }

    /**
     * Get loading statistics
     */
    public LoadingStats getLoadingStats() {
        int totalEngines = lazyEngines.size();
        int loadedEngines = (int) lazyEngines.values().stream().filter(LazyEngine::isLoaded).count();
        int totalTurbos = lazyTurbos.size();
        int loadedTurbos = (int) lazyTurbos.values().stream().filter(LazyTurbo::isLoaded).count();

        return new LoadingStats(totalEngines, loadedEngines, totalTurbos, loadedTurbos);
    }

    /**
     * Lazy engine wrapper
     */
    private static class LazyEngine {
        private final String name;
        private final Supplier<Engine> supplier;
        private volatile Engine engine;
        private volatile boolean loaded = false;

        public LazyEngine(String name, Supplier<Engine> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        public synchronized Engine load() {
            if (!loaded) {
                engine = supplier.get();
                loaded = true;
            }
            return engine;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public Engine getEngine() {
            if (!loaded) {
                throw new IllegalStateException("Engine not loaded: " + name);
            }
            return engine;
        }
    }

    /**
     * Lazy turbo wrapper
     */
    private static class LazyTurbo {
        private final String name;
        private final Supplier<Turbo> supplier;
        private volatile Turbo turbo;
        private volatile boolean loaded = false;

        public LazyTurbo(String name, Supplier<Turbo> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        public synchronized Turbo load() {
            if (!loaded) {
                turbo = supplier.get();
                loaded = true;
            }
            return turbo;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public Turbo getTurbo() {
            if (!loaded) {
                throw new IllegalStateException("Turbo not loaded: " + name);
            }
            return turbo;
        }
    }

    /**
     * Loading statistics
     */
    public record LoadingStats(int totalEngines, int loadedEngines, int totalTurbos, int loadedTurbos) {
        public double getEngineLoadRatio() {
            return totalEngines == 0 ? 0.0 : (double) loadedEngines / totalEngines;
        }

        public double getTurboLoadRatio() {
            return totalTurbos == 0 ? 0.0 : (double) loadedTurbos / totalTurbos;
        }
    }
}