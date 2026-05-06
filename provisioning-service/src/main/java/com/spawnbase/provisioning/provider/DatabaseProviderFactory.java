package com.spawnbase.provisioning.provider;
import com.spawnbase.common.model.DatabaseType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseProviderFactory {

    private final Map<DatabaseType, DatabaseProvider> providers
            = new EnumMap<>(DatabaseType.class);

    public DatabaseProviderFactory(List<DatabaseProvider> providerList) {
        for (DatabaseProvider provider : providerList) {
            providers.put(provider.getType(), provider);
        }
    }

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

    public java.util.Set<DatabaseType> getSupportedTypes() {
        return providers.keySet();
    }
}
