package com.spawnbase.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * High-level summary for the admin dashboard.
 *
 * Shown at the top of the UI as stat cards:
 * - Total instances
 * - Running instances
 * - Failed instances
 * - Breakdown by DB type
 * - Breakdown by state
 */
@Data
@Builder
public class DashboardSummary {

    private long totalInstances;
    private long runningInstances;
    private long failedInstances;
    private long provisioningInstances;
    private long stoppedInstances;

    /**
     * Count per database type.
     * e.g. { "POSTGRESQL": 3, "MYSQL": 1, "MONGODB": 2 }
     */
    private Map<String, Long> byDbType;

    /**
     * Count per state.
     * e.g. { "RUNNING": 4, "FAILED": 1, "STOPPED": 1 }
     */
    private Map<String, Long> byState;
}