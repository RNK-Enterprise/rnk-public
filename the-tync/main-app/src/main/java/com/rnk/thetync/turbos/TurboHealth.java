package com.rnk.thetync.turbos;

/**
 * Health status of a turbo.
 */
public enum TurboHealth {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN,
    OVERHEATED, // Specific to turbos for performance issues
    THROTTLED   // Specific to turbos for resource management
}
