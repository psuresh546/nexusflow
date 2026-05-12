package com.spawnbase.provisioning.provider;

import com.spawnbase.common.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * MySQL database provider.
 *
 * Docker image: mysql:8.0
 * Default port: 3306
 * Health check: mysqladmin ping
 *
 * Environment variables MySQL expects:
 * MYSQL_ROOT_PASSWORD — root user password
 * MYSQL_DATABASE      — database to create on startup
 * MYSQL_USER          — non-root user to create
 * MYSQL_PASSWORD      — non-root user password
 *
 * We use MYSQL_USER=spawnbase as the app user.
 * Root password is set separately for security.
 */
@Component
public class MySQLProvider implements DatabaseProvider {

    @Override
    public String getDockerImage() {
        return "mysql:8.0";
    }

    @Override
    public String getContainerPort() {
        return "3306";
    }

    @Override
    public String[] getEnvironmentVariables(
            String password, String dbName) {
        return new String[]{
                "MYSQL_ROOT_PASSWORD=" + password,
                "MYSQL_DATABASE=" + dbName,
                "MYSQL_USER=spawnbase",
                "MYSQL_PASSWORD=" + password
        };
    }

    /**
     * mysqladmin ping checks if MySQL is accepting connections.
     * -h 127.0.0.1 connects to the local MySQL instance.
     * --silent suppresses output — only exit code matters.
     *
     * Returns exit code 0 when healthy.
     */
    @Override
    public String[] getHealthCheckCommand() {
        return new String[]{
                "mysqladmin",
                "ping",
                "-h", "127.0.0.1",
                "--silent"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "512m";
    }

    /**
     * MySQL JDBC URL format:
     * jdbc:mysql://host:port/dbname?useSSL=false&allowPublicKeyRetrieval=true
     *
     * useSSL=false — local dev, no TLS needed
     * allowPublicKeyRetrieval=true — required for MySQL 8.0
     */
    @Override
    public String getConnectionUrl(
            String host, Integer port, String dbName) {
        return String.format(
                "jdbc:mysql://%s:%d/%s" +
                        "?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, dbName);
    }

    @Override
    public DatabaseType getSupportedType() {
        return DatabaseType.MYSQL;
    }
}