package com.spawnbase.provisioning.provider;
import com.spawnbase.provisioning.model.DatabaseType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that returns the correct DatabaseProvider
 * for a given DatabaseType.
 *
 * HOW IT WORKS — Spring magic:
 * Spring sees 3 @Component classes implementing DatabaseProvider.
 * It injects ALL of them as a List<DatabaseProvider>.
 * The factory builds a Map from that list for O(1) lookup.
 *
 * HOW TO ADD A NEW DATABASE (e.g. Redis):
 * 1. Add REDIS to DatabaseType enum
 * 2. Create RedisProvider implements DatabaseProvider
 * 3. Done. Factory picks it up automatically.
 * Zero changes to existing code.
 *
 * Interview tip: This is Dependency Injection + Strategy Pattern
 * working together. Spring wires the strategies, factory routes them.
 */
@Component
public class DatabaseProviderFactory {

    // Map from DatabaseType → its Provider
    // EnumMap for O(1) lookup with enum keys
    private final Map<DatabaseType, DatabaseProvider> providers
            = new EnumMap<>(DatabaseType.class);

    /**
     * Spring injects ALL DatabaseProvider implementations here.
     * No need to manually register them — @Component handles it.
     */
    public DatabaseProviderFactory(List<DatabaseProvider> providerList) {
        for (DatabaseProvider provider : providerList) {
            providers.put(provider.getType(), provider);
        }
    }

    /**
     * Returns the correct provider for the given database type.
     *
     * @throws IllegalArgumentException if no provider found.
     *         This means a DatabaseType was added to the enum
     *         but no @Component provider was created for it.
     *         Fail loudly — never silently ignore this.
     */
    public DatabaseProvider getProvider(DatabaseType type) {
        DatabaseProvider provider = providers.get(type);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "No provider registered for database type: " + type +
                            ". Did you forget to create a @Component for it?"
            );
        }

        return provider;
    }

    /**
     * Returns all supported database types.
     * Used by the API to tell users what databases are available.
     */
    public java.util.Set<DatabaseType> getSupportedTypes() {
        return providers.keySet();
    }
}
