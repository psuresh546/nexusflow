package com.spawnbase.provisioning.provider;
import com.spawnbase.provisioning.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * MySQL implementation of DatabaseProvider.
 *
 * Docker image : mysql:8.0
 * Default port : 3306
 * Auth env vars: MYSQL_ROOT_PASSWORD, MYSQL_DATABASE, MYSQL_USER
 */
@Component
public class MySQLProvider implements DatabaseProvider {

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String getDockerImage() {
        return "mysql:8.0";
    }

    @Override
    public int getContainerPort() {
        return 3306;
    }

    @Override
    public String[] getEnvironmentVariables(String password, String dbName) {
        return new String[]{
                "MYSQL_ROOT_PASSWORD=" + password,
                "MYSQL_DATABASE="      + dbName,
                "MYSQL_USER=spawnbase",
                "MYSQL_PASSWORD="      + password
        };
    }

    @Override
    public String[] getHealthCheckCommand() {
        // mysqladmin ping checks if MySQL server is alive
        return new String[]{
                "mysqladmin", "ping", "-h", "localhost", "--silent"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "512m";
    }

    @Override
    public String getConnectionUrl(String host, int port, String dbName) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, dbName
        );
    }
}
