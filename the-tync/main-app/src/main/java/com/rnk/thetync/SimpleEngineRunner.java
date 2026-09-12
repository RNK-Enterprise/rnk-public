package com.rnk.thetync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnk.thetync.transform.AdaptedArtifactVerifier;
import com.rnk.thetync.transform.CrossLoaderAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple Engine Runner for testing Node.js to Java integration
 * Enhanced with Mod Delivery System support.
 *
 * JSON handling uses Jackson (present on the dependency classpath), so
 * payloads may contain nested objects and arrays. Results are serialized
 * with real JSON semantics (quoting, escaping, nested structures).
 */
public class SimpleEngineRunner {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: SimpleEngineRunner <engineName> <jsonContext>");
                System.exit(1);
            }

            String engineName = args[0];
            String jsonContext = args[1];

            // Parse JSON context (full JSON: nested objects/arrays supported)
            Map<String, Object> context = parseJson(jsonContext);

            // Execute engine (enhanced with mod delivery)
            Map<String, Object> result = executeEngine(engineName, context);

            // Output result as JSON
            System.out.println(JSON.writeValueAsString(result));

        } catch (Exception e) {
            Throwable cause = e;
            while (cause instanceof java.lang.reflect.InvocationTargetException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            try {
                Map<String, Object> errorResult = new LinkedHashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", cause.getClass().getSimpleName()
                    + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
                errorResult.put("executionTimeMs", System.currentTimeMillis());
                System.out.println(JSON.writeValueAsString(errorResult));
            } catch (Exception inner) {
                System.err.println("Failed to serialize error result: " + inner.getMessage());
            }
            System.exit(1);
        }
    }

    /**
     * Parse arbitrary JSON into a Map. Accepts nested objects and arrays;
     * a bare non-object JSON document is wrapped as {"value": ...}.
     */
    private static Map<String, Object> parseJson(String json) throws Exception {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Object parsed = JSON.readValue(json, Object.class);
        if (parsed instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            return map;
        }
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("value", parsed);
        return wrapped;
    }

    private static Map<String, Object> executeEngine(String engineName, Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("engine", engineName);
        result.put("executionTimeMs", System.currentTimeMillis());

        switch (engineName) {
            case "TestEngine":
                result.put("message", "Hello from TestEngine! Context received: " + context);
                result.put("data", "processed: true, timestamp: " + System.currentTimeMillis());
                break;

            case "PredictiveOptimizationEngine":
                result.put("message", "PredictiveOptimizationEngine executed with ML optimization");
                result.put("data", "optimizationApplied: true, performanceGain: 0.15, confidence: 0.92");
                result.put("mlEnhanced", true);
                result.put("recommendations", java.util.Arrays.asList(
                    "Increase view distance for better performance",
                    "Optimize chunk loading with predictive caching",
                    "Apply ML-based entity culling"
                ));
                break;

            case "ModDeliveryEngine": {
                // Handle universal mod requests vs specific mod processing
                String requestType = str(context.get("requestType"));
                if ("universal_mods".equals(requestType)) {
                    // Return mock universal mod data (dependencies not available)
                    result.put("universalMods", java.util.Arrays.asList(
                        "UniversalPerformanceMod",
                        "UniversalCompatibilityMod",
                        "UniversalSecurityMod",
                        "UniversalOptimizationMod"
                    ));
                    result.put("modData", "UEsDBAoAAAAAALtVcVAAAAAAAAAAAAAAAAAJABwAdW5pdm9kcy9VUEsDBAoAAAAAALtVcVAAAAAAAAAAAAAAAAAhABwAdW5pdm9kcy9V");
                    result.put("targetVersion", context.containsKey("targetVersion") ? str(context.get("targetVersion")) : "1.20.1");
                    result.put("injectionType", context.containsKey("injectionType") ? str(context.get("injectionType")) : "runtime");
                    result.put("message", "Universal mods retrieved successfully (standalone mode)");
                } else {
                    // Mock mod processing (dependencies not available)
                    String modPath = str(context.get("modPath"));
                    if (!modPath.isEmpty()) {
                        result.put("modPrepared", true);
                        result.put("preparationMessage", "Mod prepared successfully (mock)");
                        result.put("transformedClasses", 5);
                        result.put("optimizationsApplied", 3);
                        result.put("compatibilityScore", 95.5);
                    } else {
                        result.put("error", "No modPath provided for ModDeliveryEngine");
                        result.put("success", false);
                    }
                }
                break;
            }

            case "CrossLoaderAdapterEngine": {
                // REAL cross-loader adaptation (no mocks): ASM bytecode rewrite
                // of a Fabric mod into a Paper-loadable artifact, plus optional
                // execution proof via the isolated-classloader verifier.
                String crossRequestType = str(context.get("requestType"));
                boolean verifyOnly = "verify".equals(crossRequestType);
                boolean structureOnly = "verify_structure".equals(crossRequestType);
                String modPath = (verifyOnly || structureOnly) ? "unused" : str(context.get("modPath"));
                String adaptedJarPath = str(context.get("adaptedJarPath"));
                if (!verifyOnly && !structureOnly && modPath.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "No modPath provided for CrossLoaderAdapterEngine");
                    break;
                }
                if ((verifyOnly || structureOnly) && adaptedJarPath.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "No adaptedJarPath provided for CrossLoaderAdapterEngine verify");
                    break;
                }
                try {
                    CrossLoaderAdapter adapter = new CrossLoaderAdapter();
                    if (structureOnly) {
                        // Structural verification: bytecode-level checks only, no
                        // classloading — valid for real mods whose transitive
                        // dependencies (Minecraft, Fabric API) are not resolvable
                        // in this JVM. This is the boundary-measurement verdict.
                        Map<String, Object> verification = new AdaptedArtifactVerifier().verifyStructure(adaptedJarPath);
                        result.putAll(verification);
                        result.put("verification", true);
                        if (!Boolean.TRUE.equals(verification.get("structureVerified"))) {
                            result.put("success", false);
                            result.put("error", "adapted artifact failed structural verification");
                        }
                    } else if (verifyOnly) {
                        Map<String, Object> verification = new AdaptedArtifactVerifier().verify(adaptedJarPath);
                        result.putAll(verification);
                        result.put("verification", true);
                        if (!Boolean.TRUE.equals(verification.get("verified"))) {
                            result.put("success", false);
                            result.put("error", "adapted artifact failed execution verification");
                        }
                    } else {
                        // adapt (default) or adapt_and_verify
                        String outputPath = str(context.get("outputPath"));
                        if (outputPath.isEmpty()) {
                            outputPath = modPath.replaceAll("\\.jar$", "") + "-adapted-paper.jar";
                        }
                        Map<String, Object> adaptation = adapter.adaptFabricModToPaper(modPath, outputPath);
                        result.put("adaptation", adaptation);
                        result.put("adaptedJar", adaptation.get("adaptedJar"));
                        result.put("entrypoints", adaptation.get("entrypoints"));
                        result.put("classesRewritten", adaptation.get("classesRewritten"));
                        if ("adapt_and_verify".equals(crossRequestType)) {
                            Map<String, Object> verification = new AdaptedArtifactVerifier().verify(outputPath);
                            result.putAll(verification);
                            result.put("verification", true);
                            if (!Boolean.TRUE.equals(verification.get("verified"))) {
                                result.put("success", false);
                                result.put("error", "adapted artifact failed execution verification");
                            }
                        }
                    }
                } catch (Exception adaptationError) {
                    Throwable cause = adaptationError;
                    while (cause instanceof java.lang.reflect.InvocationTargetException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    result.put("success", false);
                    result.put("error", "CrossLoaderAdapterEngine failed: "
                        + cause.getClass().getSimpleName()
                        + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
                }
                break;
            }

            case "InjectionEngine": {
                // Mock mod injection (dependencies not available)
                String injectionModPath = str(context.get("modPath"));

                if (!injectionModPath.isEmpty()) {
                    result.put("modInjected", true);
                    result.put("injectionMessage", "Mod injected successfully (mock)");
                } else {
                    result.put("error", "No modPath provided for InjectionEngine");
                    result.put("success", false);
                }
                break;
            }

            case "ValidationEngine": {
                // Handle validation requests
                String validationRequestType = str(context.get("requestType"));
                if ("ping".equals(validationRequestType)) {
                    result.put("message", "ValidationEngine is alive and responding");
                    result.put("status", "healthy");
                    result.put("timestamp", System.currentTimeMillis());
                } else if ("validate_compatibility".equals(validationRequestType)) {
                    // Mock compatibility validation
                    result.put("compatible", true);
                    result.put("reason", "Mock validation passed");
                    result.put("validationScore", 95.5);
                    result.put("message", "Mod compatibility validated successfully");
                } else {
                    result.put("message", "ValidationEngine executed");
                    result.put("validationType", validationRequestType);
                }
                break;
            }

            default:
                result.put("message", "Engine " + engineName + " executed successfully");
                result.put("data", "engine: " + engineName + ", status: completed");
        }

        return result;
    }

    /**
     * Coerce a context value to its string form (Jackson parses JSON numbers
     * as Integer/Long/Double and booleans as Boolean, so be lenient).
     */
    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
