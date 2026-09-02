package com.ld.thetync.engines;

/**
 * Health status of an engine.
 */
public class EngineHealth {
    private Status status;
    private long lastCheckTime;
    private String details;

    public enum Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }

    // Static factory methods for backward compatibility
    public static final EngineHealth HEALTHY = new EngineHealth(Status.HEALTHY, "Engine is healthy");
    public static final EngineHealth DEGRADED = new EngineHealth(Status.DEGRADED, "Engine is degraded");
    public static final EngineHealth UNHEALTHY = new EngineHealth(Status.UNHEALTHY, "Engine is unhealthy");
    public static final EngineHealth UNKNOWN = new EngineHealth(Status.UNKNOWN, "Engine status unknown");

    public EngineHealth() {
        this.status = Status.UNKNOWN;
        this.lastCheckTime = System.currentTimeMillis();
        this.details = "Not checked yet";
    }

    public EngineHealth(Status status, String details) {
        this.status = status;
        this.lastCheckTime = System.currentTimeMillis();
        this.details = details;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.lastCheckTime = System.currentTimeMillis();
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Check if the engine is healthy
     */
    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }

    @Override
    public String toString() {
        return String.format("EngineHealth{status=%s, lastCheck=%d, details='%s'}",
                           status, lastCheckTime, details);
    }
}