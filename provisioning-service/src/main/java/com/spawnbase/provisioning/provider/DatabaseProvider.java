package com.spawnbase.provisioning.provider;

import com.spawnbase.common.model.DatabaseType;

/**
 * Strategy interface for database providers.
 *
 * Each implementation provides Docker-specific config
 * for a particular database type.
 *
 * Pattern: Strategy — behaviour varies by DB type,
 * the algorithm (provisioning flow) stays the same.
 */
public interface DatabaseProvider {

    /** Docker image to pull and run */
    String getDockerImage();

    /** Port exposed inside the container */
    String getContainerPort();

    /**
     * Environment variables passed to the container.
     * Each DB has different variable names.
     */
    String[] getEnvironmentVariables(
            String password, String dbName);

    /**
     * Command to run inside container to check health.
     * Used in Docker healthcheck config.
     */
    String[] getHealthCheckCommand();

    /** Memory limit for the container e.g. "512m" */
    String getMemoryLimit();

    /**
     * Connection URL for the database.
     * Format varies by DB type:
     * PostgreSQL: jdbc:postgresql://...
     * MySQL:      jdbc:mysql://...
     * MongoDB:    mongodb://...
     */
    String getConnectionUrl(
            String host, Integer port, String dbName);

    /** Which DatabaseType this provider handles */
    DatabaseType getSupportedType();
}