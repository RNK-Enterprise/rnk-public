package com.ld.thetync.engines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DataSerializationEngine}: lifecycle, serialize/deserialize/
 * convert operations, formats, compression, and every failure branch.
 */
class DataSerializationEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private DataSerializationEngine newEngine() {
        return new DataSerializationEngine(new SimpleMeterRegistry());
    }

    private DataSerializationEngine initializedEngine() {
        DataSerializationEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);
        return engine;
    }

    private EngineContext contextFor(String operation, Object data, String format, Boolean compress) {
        EngineContext context = new EngineContext();
        context.put("operation", operation);
        context.put("data", data);
        if (format != null || compress != null) {
            if (format != null) context.put("format", format);
            if (compress != null) context.put("compress", compress);
        }
        return context;
    }

    @Test
    void executeBeforeInitializeFails() {
        DataSerializationEngine engine = newEngine();
        assertThrows(IllegalStateException.class,
                () -> engine.execute(contextFor("serialize", Map.of(), "json", null)).block(TIMEOUT));
    }

    @Test
    void initializeReportsHealthy() {
        DataSerializationEngine engine = newEngine();
        engine.initialize().block(TIMEOUT);

        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertEquals("DataSerializationEngine", engine.getName());
        assertEquals("1.0.0", engine.getVersion());
        assertEquals(engine.getMetrics(), engine.getMetrics());
    }

    @Test
    void initializeMarksUnhealthyWhenSelfTestFails() throws Exception {
        DataSerializationEngine engine = newEngine();
        // The mapper serializes fine but deserializes into a mismatched map,
        // which trips the engine's round-trip self-test.
        ObjectMapper lying = new ObjectMapper() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T readValue(String content, Class<T> valueType) {
                return (T) new HashMap<String, Object>();
            }
        };
        Field mapperField = DataSerializationEngine.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(engine, lying);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> engine.initialize().block(TIMEOUT));
        assertTrue(ex.getCause() instanceof Exception);
        assertEquals(EngineHealth.UNHEALTHY, engine.getHealth());
    }

    @Test
    void serializeJsonWithoutCompression() {
        DataSerializationEngine engine = initializedEngine();
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", "value");

        EngineResult result = engine.execute(contextFor("serialize", payload, "json", null)).block(TIMEOUT);

        assertTrue(result.success());
        String json = new String((byte[]) result.data(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"key\""));
        assertTrue(json.contains("\"value\""));
    }

    @Test
    void serializeWithNullFormatUsesJson() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("serialize", Map.of("n", 1), null, null)).block(TIMEOUT);

        assertTrue(result.success());
        String json = new String((byte[]) result.data(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"n\""));
    }

    @Test
    void serializeDeserializeRoundTripWithCompression() {
        DataSerializationEngine engine = initializedEngine();
        Map<String, Object> payload = new HashMap<>();
        payload.put("round", "trip");

        EngineResult serialized = engine.execute(contextFor("serialize", payload, "json", true)).block(TIMEOUT);
        assertTrue(serialized.success());

        EngineContext deserializeContext = new EngineContext();
        deserializeContext.put("operation", "deserialize");
        deserializeContext.put("data", serialized.data());
        deserializeContext.put("format", "json");
        deserializeContext.put("compress", true);
        EngineResult deserialized = engine.execute(deserializeContext).block(TIMEOUT);

        assertTrue(deserialized.success());
        assertEquals(payload, deserialized.data());
    }

    @Test
    void serializeProtobufMessageDelegatesToMessage() {
        DataSerializationEngine engine = initializedEngine();
        byte[] expected = {1, 2, 3};
        Message message = mock(Message.class);
        when(message.toByteArray()).thenReturn(expected);

        EngineResult result = engine.execute(contextFor("serialize", message, "protobuf", null)).block(TIMEOUT);

        assertTrue(result.success());
        assertArrayEquals(expected, (byte[]) result.data());
    }

    @Test
    void serializeProtobufRequiresMessageObject() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("serialize", "not-a-message", "protobuf", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Protobuf format requires Message object", result.errorMessage());
    }

    @Test
    void serializeUnsupportedFormatFails() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("serialize", Map.of(), "xml", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Unsupported format: xml", result.errorMessage());
    }

    @Test
    void deserializeFromByteArray() {
        DataSerializationEngine engine = initializedEngine();
        byte[] json = "{\"a\":5}".getBytes(StandardCharsets.UTF_8);

        EngineResult result = engine.execute(contextFor("deserialize", json, "json", null)).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.data();
        assertEquals(5, parsed.get("a"));
    }

    @Test
    void deserializeFromStringInput() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("deserialize", "{\"b\":6}", "json", null)).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.data();
        assertEquals(6, parsed.get("b"));
    }

    @Test
    void deserializeWithNullFormatDefaultsToJson() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("deserialize", "{\"f\":9}", null, null)).block(TIMEOUT);

        assertTrue(result.success());
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.data();
        assertEquals(9, parsed.get("f"));
    }

    @Test
    void deserializeProtobufReturnsRawBytes() {
        DataSerializationEngine engine = initializedEngine();
        byte[] raw = {9, 8, 7};

        EngineResult result = engine.execute(contextFor("deserialize", raw, "protobuf", null)).block(TIMEOUT);

        assertTrue(result.success());
        assertSame(raw, result.data());
    }

    @Test
    void deserializeInvalidTypeFails() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("deserialize", 42, "json", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Data must be byte[] or String for deserialization", result.errorMessage());
    }

    @Test
    void deserializeUnsupportedFormatFails() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("deserialize", "{}", "yaml", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Unsupported format: yaml", result.errorMessage());
    }

    @Test
    void convertToJsonReturnsString() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("convert", Map.of("c", 7), "json", null)).block(TIMEOUT);

        assertTrue(result.success());
        assertTrue(result.data() instanceof String);
        assertTrue(((String) result.data()).contains("\"c\""));
    }

    @Test
    void convertToBytesFromStringReturnsUtf8() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("convert", "text", "bytes", null)).block(TIMEOUT);

        assertTrue(result.success());
        assertArrayEquals("text".getBytes(StandardCharsets.UTF_8), (byte[]) result.data());
    }

    @Test
    void convertToBytesFromObjectReturnsSerializedBytes() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("convert", Map.of("d", 8), "bytes", null)).block(TIMEOUT);

        assertTrue(result.success());
        assertTrue(result.data() instanceof byte[]);
    }

    @Test
    void convertUnsupportedTargetFormatFails() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("convert", Map.of(), "xml", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Unsupported target format: xml", result.errorMessage());
    }

    @Test
    void unknownOperationFails() {
        DataSerializationEngine engine = initializedEngine();

        EngineResult result = engine.execute(contextFor("transmogrify", Map.of(), "json", null)).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Unknown operation: transmogrify", result.errorMessage());
    }

    @Test
    void missingOperationFails() {
        DataSerializationEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("data", Map.of());

        EngineResult result = engine.execute(context).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Missing required parameters: operation or data", result.errorMessage());
    }

    @Test
    void missingDataFails() {
        DataSerializationEngine engine = initializedEngine();
        EngineContext context = new EngineContext();
        context.put("operation", "serialize");

        EngineResult result = engine.execute(context).block(TIMEOUT);

        assertFalse(result.success());
        assertEquals("Missing required parameters: operation or data", result.errorMessage());
    }

    @Test
    void shutdownResetsState() {
        DataSerializationEngine engine = initializedEngine();
        engine.shutdown().block(TIMEOUT);

        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());
        assertThrows(IllegalStateException.class,
                () -> engine.execute(contextFor("serialize", Map.of(), "json", null)).block(TIMEOUT));
    }
}
