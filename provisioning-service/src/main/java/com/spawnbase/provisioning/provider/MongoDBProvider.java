package com.spawnbase.provisioning.provider;

import com.spawnbase.common.model.DatabaseType;
import org.springframework.stereotype.Component;

/**
 * MongoDB database provider.
 *
 * Docker image: mongo:7.0
 * Default port: 27017
 * Health check: mongosh --eval "db.adminCommand('ping')"
 *
 * Environment variables MongoDB expects:
 * MONGO_INITDB_ROOT_USERNAME — root username
 * MONGO_INITDB_ROOT_PASSWORD — root password
 * MONGO_INITDB_DATABASE      — initial database to create
 *
 * MongoDB connection URLs use a different format than JDBC.
 * The MongoDB URI format is:
 * mongodb://username:password@host:port/dbname
 */
@Component
public class MongoDBProvider implements DatabaseProvider {

    @Override
    public String getDockerImage() {
        return "mongo:7.0";
    }

    @Override
    public String getContainerPort() {
        return "27017";
    }

    @Override
    public String[] getEnvironmentVariables(
            String password, String dbName) {
        return new String[]{
                "MONGO_INITDB_ROOT_USERNAME=spawnbase",
                "MONGO_INITDB_ROOT_PASSWORD=" + password,
                "MONGO_INITDB_DATABASE=" + dbName
        };
    }

    /**
     * mongosh --eval "db.adminCommand('ping')" checks
     * if MongoDB is accepting connections.
     *
     * mongosh is the modern MongoDB shell (replaces mongo CLI).
     * --quiet suppresses banner output.
     * Exit code 0 = healthy.
     */
    @Override
    public String[] getHealthCheckCommand() {
        return new String[]{
                "mongosh",
                "--quiet",
                "--eval",
                "db.adminCommand('ping')"
        };
    }

    @Override
    public String getMemoryLimit() {
        return "512m";
    }

    /**
     * MongoDB connection URI format:
     * mongodb://username:password@host:port/dbname?authSource=admin
     *
     * authSource=admin — credentials are stored in admin DB
     * This is required when using MONGO_INITDB_ROOT_USERNAME.
     */
    @Override
    public String getConnectionUrl(
            String host, Integer port, String dbName) {
        return String.format(
                "mongodb://spawnbase:%s@%s:%d/%s?authSource=admin",
                // Note: in production, password would be URL-encoded
                // to handle special characters safely
                "REDACTED", host, port, dbName);
    }

    @Override
    public DatabaseType getSupportedType() {
        return DatabaseType.MONGODB;
    }
}