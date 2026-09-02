package com.ld.thetync.turbos;

/**
 * Result of a turbo execution.
 */
public class TurboResult {

    private final boolean success;
    private final Object data;
    private final String errorMessage;
    private final long executionTimeMs;
    private final double accelerationFactor;

    public TurboResult(boolean success, Object data, String errorMessage, long executionTimeMs, double accelerationFactor) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
        this.accelerationFactor = accelerationFactor;
    }

    public static TurboResult success(Object data, long executionTimeMs, double accelerationFactor) {
        return new TurboResult(true, data, null, executionTimeMs, accelerationFactor);
    }

    public static TurboResult failure(String errorMessage, long executionTimeMs, double accelerationFactor) {
        return new TurboResult(false, null, errorMessage, executionTimeMs, accelerationFactor);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public double getAccelerationFactor() {
        return accelerationFactor;
    }
}
