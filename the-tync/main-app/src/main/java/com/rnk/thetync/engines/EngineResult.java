package com.rnk.thetync.engines;

/**
 * Result of an engine execution.
 */
public record EngineResult(boolean success, Object data, String errorMessage, long executionTimeMs) {

    public static EngineResult success(Object data, long executionTimeMs) {
        return new EngineResult(true, data, null, executionTimeMs);
    }

    public static EngineResult failure(String errorMessage, long executionTimeMs) {
        return new EngineResult(false, null, errorMessage, executionTimeMs);
    }
}