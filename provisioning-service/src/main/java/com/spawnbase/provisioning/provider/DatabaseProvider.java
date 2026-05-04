package com.spawnbase.provisioning.provider;
import com.spawnbase.provisioning.model.DatabaseType;

/**
 * Strategy Pattern contract.
 *
 * Every supported database MUST implement this interface.
 * The provisioning engine only talks to this interface —
 * it never knows which database it's actually working with.
 *
 * This is the Open/Closed Principle in action:
 * - Open for extension  → add MongoDB? create MongoDBProvider
 * - Closed for modification → existing code never changes
 *
 * Interview tip: This interface is the ONLY thing
 * ProvisioningService depends on. Not PostgreSQL. Not MySQL.
 * Just DatabaseProvider.
 */
public interface DatabaseProvider {

    /**
     * Which database type does this provider handle?
     * Used by the factory to find the right provider.
     */
    DatabaseType getType();

    /**
     * The Docker image to pull for this database.
     * e.g. "postgres:15", "mysql:8.0", "mongo:7.0"
     */
    String getDockerImage();

    /**
     * The port this database listens on inside the container.
     * e.g. PostgreSQL=5432, MySQL=3306, MongoDB=27017
     */
    int getContainerPort();

    /**
     * Environment variables to pass to the Docker container.
     * e.g. POSTGRES_PASSWORD, MYSQL_ROOT_PASSWORD, etc.
     *
     * @param password  the generated password for this instance
     * @param dbName    the database name to create
     * @return          array of "KEY=VALUE" strings
     */
    String[] getEnvironmentVariables(String password, String dbName);

    /**
     * A health check command to verify the DB is ready.
     * Docker runs this inside the container to know when
     * the DB is accepting connections.
     *
     * @return  command array for docker health check
     */
    String[] getHealthCheckCommand();

    /**
     * Max memory this DB container should use.
     * Prevents one instance from eating all host memory.
     * e.g. "512m", "1g"
     */
    default String getMemoryLimit() {
        return "512m";    // safe default for all DBs
    }

    /**
     * JDBC/connection URL format for this database.
     * Used by credential-service to store the connection string.
     *
     * @param host      Docker host IP
     * @param port      mapped host port
     * @param dbName    database name
     */
    String getConnectionUrl(String host, int port, String dbName);
}
