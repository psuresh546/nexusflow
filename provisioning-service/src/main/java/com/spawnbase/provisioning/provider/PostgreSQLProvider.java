package com.spawnbase.provisioning.provider;
import com.spawnbase.common.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL implementation of DatabaseProvider.
 *
 * Docker image : postgres:15
 * Default port : 5432
 * Auth env vars: POSTGRES_PASSWORD, POSTGRES_DB, POSTGRES_USER
 */
@Component
public class PostgreSQLProvider implements DatabaseProvider {

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String getDockerImage() {
        return "postgres:15";
    }

    @Override
    public int getContainerPort() {
        return 5432;
    }

    @Override
    public String[] getEnvironmentVariables(String password, String dbName) {
        return new String[]{
                "POSTGRES_PASSWORD=" + password,
                "POSTGRES_DB="       + dbName,
                "POSTGRES_USER=spawnbase"
        };
    }

    @Override
    public String[] getHealthCheckCommand() {
        // pg_isready checks if PostgreSQL is accepting connections
        // -U = username, -d = database name
        return new String[]{
                "pg_isready", "-U", "spawnbase"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "512m";
    }

    @Override
    public String getConnectionUrl(String host, int port, String dbName) {
        return String.format(
                "jdbc:postgresql://%s:%d/%s", host, port, dbName
        );
    }
}
