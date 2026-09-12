package com.rnk.thetync.turbos;

/**
 * Health status of a turbo.
 */
public class TurboHealth {
    private Status status;
    private long lastCheckTime;
    private String details;

    public enum Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN,
        OVERHEATED, // Specific to turbos for performance issues
        THROTTLED   // Specific to turbos for resource management
    }

    // Static factory methods for backward compatibility
    public static final TurboHealth HEALTHY = new TurboHealth(Status.HEALTHY, "Turbo is healthy");
    public static final TurboHealth DEGRADED = new TurboHealth(Status.DEGRADED, "Turbo is degraded");
    public static final TurboHealth UNHEALTHY = new TurboHealth(Status.UNHEALTHY, "Turbo is unhealthy");
    public static final TurboHealth UNKNOWN = new TurboHealth(Status.UNKNOWN, "Turbo status unknown");
    public static final TurboHealth OVERHEATED = new TurboHealth(Status.OVERHEATED, "Turbo is overheated");
    public static final TurboHealth THROTTLED = new TurboHealth(Status.THROTTLED, "Turbo is throttled");

    public TurboHealth() {
        this.status = Status.UNKNOWN;
        this.lastCheckTime = System.currentTimeMillis();
        this.details = "Not checked yet";
    }

    public TurboHealth(Status status, String details) {
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
     * Check if the turbo is healthy
     */
    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }

    @Override
    public String toString() {
        return String.format("TurboHealth{status=%s, lastCheck=%d, details='%s'}",
                           status, lastCheckTime, details);
    }
}
