package com.spawnbase.provisioning.model;

/**
 * Supported database types in SpawnBase.
 * Adding a new DB type = add it here + create a new Provider class.
 * Nothing else changes.
 */
public enum DatabaseType {
    POSTGRESQL,
    MYSQL,
    MONGODB
}
