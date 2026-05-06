package com.spawnbase.provisioning.provider;
import com.spawnbase.common.model.DatabaseType;

public interface DatabaseProvider {

    DatabaseType getType();

    String getDockerImage();

    int getContainerPort();

    String[] getEnvironmentVariables(String password, String dbName);

    String[] getHealthCheckCommand();

    default String getMemoryLimit() {
        return "512m";    // safe default for all DBs
    }

    String getConnectionUrl(String host, int port, String dbName);
}
