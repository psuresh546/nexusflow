package com.spawnbase.provisioning.provider;

import com.spawnbase.common.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL database provider.
 *
 * Docker image: postgres:15
 * Default port: 5432
 * Health check: pg_isready
 */
@Component
public class PostgreSQLProvider implements DatabaseProvider {

    @Override
    public String getDockerImage() {
        return "postgres:15";
    }

    @Override
    public String getContainerPort() {
        return "5432";
    }

    @Override
    public String[] getEnvironmentVariables(
            String password, String dbName) {
        return new String[]{
                "POSTGRES_USER=spawnbase",
                "POSTGRES_PASSWORD=" + password,
                "POSTGRES_DB=" + dbName
        };
    }

    /**
     * pg_isready checks if PostgreSQL is accepting connections.
     * -U spawnbase — connect as the spawnbase user.
     * Returns exit code 0 when the server is ready.
     */
    @Override
    public String[] getHealthCheckCommand() {
        return new String[]{
                "pg_isready",
                "-U", "spawnbase"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "512m";
    }

    @Override
    public String getConnectionUrl(
            String host, Integer port, String dbName) {
        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                host, port, dbName);
    }

    @Override
    public DatabaseType getSupportedType() {
        return DatabaseType.POSTGRESQL;
    }
}