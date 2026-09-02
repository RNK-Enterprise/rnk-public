package com.ld.thetync.engines;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics for an engine.
 */
public class EngineMetrics {

    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong averageExecutionTimeMs = new AtomicLong(0);

    public void recordExecution(boolean success, long executionTimeMs) {
        totalExecutions.incrementAndGet();
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        averageExecutionTimeMs.set(totalExecutionTimeMs.get() / totalExecutions.get());
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
}