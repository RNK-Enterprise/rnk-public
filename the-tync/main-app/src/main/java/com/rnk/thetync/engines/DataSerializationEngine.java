package com.rnk.thetync.engines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Data Serialization Engine - Handles cross-loader data exchange.
 * Uses Jackson and Protocol Buffers for efficient serialization.
 */
public class DataSerializationEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(DataSerializationEngine.class);

    private final String name = "DataSerializationEngine";
    private final String version = "1.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper;
    private final Cache<String, byte[]> serializationCache;

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;

    public DataSerializationEngine(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper for better performance
        objectMapper.findAndRegisterModules();

        this.serializationCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(10, TimeUnit.MINUTES)
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
                logger.info("Initializing Data Serialization Engine v{}", version);

                // Test serialization capabilities
                testSerialization();

                health = EngineHealth.HEALTHY;
                initialized = true;

                logger.info("Data Serialization Engine initialized successfully");
                return null;
            } catch (Exception e) {
                logger.error("Failed to initialize Data Serialization Engine", e);
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
                logger.debug("Executing data serialization");

                String operation = context.get("operation", String.class);
                Object data = context.get("data");
                String format = context.get("format", String.class);
                Boolean compress = context.get("compress", Boolean.class);

                if (operation == null || data == null) {
                    throw new IllegalArgumentException("Missing required parameters: operation or data");
                }

                Object result = performSerialization(operation, data, format, compress != null ? compress : false);

                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(true, executionTime);

                sample.stop(Timer.builder("engine.execution")
                        .tag("engine", name)
                        .tag("operation", operation)
                        .tag("format", format != null ? format : "default")
                        .register(meterRegistry));

                return EngineResult.success(result, executionTime);

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(false, executionTime);

                sample.stop(Timer.builder("engine.execution.error")
                        .tag("engine", name)
                        .register(meterRegistry));

                logger.error("Data serialization failed", e);
                return EngineResult.failure(e.getMessage(), executionTime);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            logger.info("Shutting down Data Serialization Engine");
            serializationCache.invalidateAll();
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

    private void testSerialization() throws Exception {
        // Test basic serialization
        Map<String, Object> testData = Map.of("test", "value", "number", 42);
        String json = objectMapper.writeValueAsString(testData);
        Map<String, Object> deserialized = objectMapper.readValue(json, Map.class);

        if (!testData.equals(deserialized)) {
            throw new Exception("Serialization test failed");
        }
    }

    private Object performSerialization(String operation, Object data, String format, boolean compress) throws Exception {
        switch (operation.toLowerCase()) {
            case "serialize":
                return serialize(data, format, compress);
            case "deserialize":
                return deserialize(data, format, compress);
            case "convert":
                return convert(data, format);
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private byte[] serialize(Object data, String format, boolean compress) throws Exception {
        byte[] serializedData;

        if ("json".equalsIgnoreCase(format) || format == null) {
            String json = objectMapper.writeValueAsString(data);
            serializedData = json.getBytes("UTF-8");
        } else if ("protobuf".equalsIgnoreCase(format)) {
            // Assume data is a protobuf message
            if (data instanceof Message) {
                serializedData = ((Message) data).toByteArray();
            } else {
                throw new IllegalArgumentException("Protobuf format requires Message object");
            }
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        if (compress) {
            serializedData = compressData(serializedData);
        }

        return serializedData;
    }

    private Object deserialize(Object data, String format, boolean compress) throws Exception {
        byte[] rawData;

        if (data instanceof byte[]) {
            rawData = (byte[]) data;
        } else if (data instanceof String) {
            rawData = ((String) data).getBytes("UTF-8");
        } else {
            throw new IllegalArgumentException("Data must be byte[] or String for deserialization");
        }

        if (compress) {
            rawData = decompressData(rawData);
        }

        if ("json".equalsIgnoreCase(format) || format == null) {
            String json = new String(rawData, "UTF-8");
            return objectMapper.readValue(json, Object.class);
        } else if ("protobuf".equalsIgnoreCase(format)) {
            // This would require specific protobuf message parsing
            // For now, return raw bytes
            return rawData;
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private Object convert(Object data, String targetFormat) throws Exception {
        // Convert between formats
        if ("json".equalsIgnoreCase(targetFormat)) {
            return objectMapper.writeValueAsString(data);
        } else if ("bytes".equalsIgnoreCase(targetFormat)) {
            if (data instanceof String) {
                return ((String) data).getBytes("UTF-8");
            } else {
                return objectMapper.writeValueAsBytes(data);
            }
        } else {
            throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
        }
    }

    private byte[] compressData(byte[] data) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(data);
        }
        return outputStream.toByteArray();
    }

    private byte[] decompressData(byte[] data) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }
}