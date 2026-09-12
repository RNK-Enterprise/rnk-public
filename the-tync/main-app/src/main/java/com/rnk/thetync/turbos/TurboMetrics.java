package com.rnk.thetync.turbos;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics for a turbo with acceleration tracking.
 */
public class TurboMetrics {

    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong averageExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong totalAccelerationFactor = new AtomicLong(0);
    private final AtomicLong averageAccelerationFactor = new AtomicLong(0);

    public void recordExecution(boolean success, long executionTimeMs, double accelerationFactor) {
        totalExecutions.incrementAndGet();
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        averageExecutionTimeMs.set(totalExecutionTimeMs.get() / totalExecutions.get());

        totalAccelerationFactor.addAndGet((long)(accelerationFactor * 1000)); // Store as milli-factor
        averageAccelerationFactor.set(totalAccelerationFactor.get() / totalExecutions.get());
    }

    public long getTotalExecutions() {
        return totalExecutions.get();
    }

    public long getSuccessfulExecutions() {
        return successfulExecutions.get();
    }

    public long getFailedExecutions() {
        return failedExecutions.get();
    }

    public double getSuccessRate() {
        long total = totalExecutions.get();
        return total == 0 ? 0.0 : (double) successfulExecutions.get() / total;
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs.get();
    }

    public long getAverageExecutionTimeMs() {
        return averageExecutionTimeMs.get();
    }

    public double getAverageAccelerationFactor() {
        return (double) averageAccelerationFactor.get() / 1000.0;
    }
}
