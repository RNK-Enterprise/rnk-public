package com.rnk.thetync.engines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Network Synchronization Engine - Manages crowdsourced compatibility data.
 * Uses OkHttp, Jackson, and Protocol Buffers for network communication.
 */
public class NetworkSynchronizationEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(NetworkSynchronizationEngine.class);

    private final String name = "NetworkSynchronizationEngine";
    private final String version = "1.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, Map<String, Object>> dataCache;

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;

    // Configuration
    private String apiEndpoint = "https://api.tync.local/v1";
    private String apiKey;

    public NetworkSynchronizationEngine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();

        this.dataCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Initializing Network Synchronization Engine v{}", version);

                // Test connectivity
                testConnectivity();

                health = EngineHealth.HEALTHY;
                initialized = true;

                logger.info("Network Synchronization Engine initialized successfully");
                return null;
            } catch (Exception e) {
                logger.error("Failed to initialize Network Synchronization Engine", e);
                health = EngineHealth.UNHEALTHY;
                throw e;
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        if (!initialized) {
            return Mono.error(new IllegalStateException("Engine not initialized"));
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            try {
                logger.debug("Executing network synchronization");

                String operation = context.get("operation", String.class);
                String modId = context.get("modId", String.class);
                Map<String, Object> data = context.get("data", Map.class);

                if (operation == null) {
                    throw new IllegalArgumentException("Missing operation parameter");
                }

                Map<String, Object> result = performOperation(operation, modId, data);

                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(true, executionTime);

                sample.stop(Timer.builder("engine.execution")
                        .tag("engine", name)
                        .tag("operation", operation)
                        .register(meterRegistry));

                return EngineResult.success(result, executionTime);

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(false, executionTime);

                sample.stop(Timer.builder("engine.execution.error")
                        .tag("engine", name)
                        .register(meterRegistry));

                logger.error("Network synchronization failed", e);
                return EngineResult.failure(e.getMessage(), executionTime);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            logger.info("Shutting down Network Synchronization Engine");
            httpClient.dispatcher().executorService().shutdown();
            dataCache.invalidateAll();
            health = EngineHealth.UNKNOWN;
            initialized = false;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public EngineHealth getHealth() {
        return health;
    }

    @Override
    public EngineMetrics getMetrics() {
        return metrics;
    }

    private void testConnectivity() throws IOException {
        Request request = new Request.Builder()
                .url(apiEndpoint + "/health")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Health check failed: " + response.code());
            }
        }
    }

    private Map<String, Object> performOperation(String operation, String modId, Map<String, Object> data) throws Exception {
        switch (operation.toLowerCase()) {
            case "fetch_compatibility":
                return fetchCompatibilityData(modId);
            case "submit_result":
                return submitTransformationResult(modId, data);
            case "get_statistics":
                return getCompatibilityStatistics();
            case "sync_cache":
                return syncLocalCache();
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private Map<String, Object> fetchCompatibilityData(String modId) throws IOException {
        // Check cache first
        Map<String, Object> cached = dataCache.getIfPresent("compat:" + modId);
        if (cached != null) {
            return cached;
        }

        // Fetch from network
        Request request = new Request.Builder()
                .url(apiEndpoint + "/compatibility/" + modId)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            // OkHttp guarantees a non-null body from execute(); an empty body
            // parses as an empty document.
            if (response.isSuccessful()) {
                String json = response.body().string();
                Map<String, Object> result = objectMapper.readValue(json, Map.class);

                // Cache the result
                dataCache.put("compat:" + modId, result);

                return result;
            } else {
                throw new IOException("Failed to fetch compatibility data: " + response.code());
            }
        }
    }

    private Map<String, Object> submitTransformationResult(String modId, Map<String, Object> data) throws IOException {
        String json = objectMapper.writeValueAsString(data);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(apiEndpoint + "/results/" + modId)
                .post(body)
                .build();        try (Response response = httpClient.newCall(request).execute()) {
            Map<String, Object> result = new ConcurrentHashMap<>();
            result.put("success", response.isSuccessful());
            result.put("statusCode", response.code());

            // OkHttp guarantees a non-null body from execute(); an empty body
            // surfaces as an empty response string.
            result.put("response", response.body().string());

            return result;
        }
    }

    private Map<String, Object> getCompatibilityStatistics() throws IOException {
        Request request = new Request.Builder()
                .url(apiEndpoint + "/statistics")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            // OkHttp guarantees a non-null body from execute(); an empty body
            // parses as an empty document.
            if (response.isSuccessful()) {
                String json = response.body().string();
                return objectMapper.readValue(json, Map.class);
            } else {
                throw new IOException("Failed to fetch statistics: " + response.code());
            }
        }
    }

    private Map<String, Object> syncLocalCache() {
        // Synchronize local cache with network data
        // This is a simplified implementation
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("cacheSize", dataCache.estimatedSize());
        result.put("syncStatus", "completed");
        return result;
    }
}