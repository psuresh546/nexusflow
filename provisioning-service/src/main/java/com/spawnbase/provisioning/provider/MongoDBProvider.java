package com.spawnbase.provisioning.provider;
import com.spawnbase.provisioning.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * MongoDB implementation of DatabaseProvider.
 *
 * Docker image : mongo:7.0
 * Default port : 27017
 * Auth env vars: MONGO_INITDB_ROOT_USERNAME, MONGO_INITDB_ROOT_PASSWORD
 *
 * Note: MongoDB uses a different connection URL format — not JDBC.
 * It uses the MongoDB connection string format (mongodb://)
 */
@Component
public class MongoDBProvider implements DatabaseProvider {

    @Override
    public DatabaseType getType() {
        return DatabaseType.MONGODB;
    }

    @Override
    public String getDockerImage() {
        return "mongo:7.0";
    }

    @Override
    public int getContainerPort() {
        return 27017;
    }

    @Override
    public String[] getEnvironmentVariables(String password, String dbName) {
        return new String[]{
                "MONGO_INITDB_ROOT_USERNAME=spawnbase",
                "MONGO_INITDB_ROOT_PASSWORD=" + password,
                "MONGO_INITDB_DATABASE="      + dbName
        };
    }

    @Override
    public String[] getHealthCheckCommand() {
        // mongosh ping checks if MongoDB is ready
        return new String[]{
                "mongosh", "--eval", "db.adminCommand('ping')"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "256m";   // MongoDB is lighter than PostgreSQL/MySQL
    }

    @Override
    public String getConnectionUrl(String host, int port, String dbName) {
        return String.format(
                "mongodb://spawnbase@%s:%d/%s", host, port, dbName
        );
    }
}
