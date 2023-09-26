package io.quarkus.amazon.secretsmanager.config.runtime;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.FilterNameStringType;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;

public abstract class SecretsManagerConfigSourceProvider implements ConfigSourceProvider {

    private SecretsManagerConfigConfig config;
    private int ordinal;

    public SecretsManagerConfigSourceProvider(
            SecretsManagerConfigConfig config,
            int ordinal) {
        this.config = config;
        this.ordinal = ordinal;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        List<ConfigSource> result = new ArrayList<>();

        // Retrieve all secrets using pagination and stream
        Map<String, String> secretsMap = new HashMap<>();
        if (config.filter().enabled()) {
            fetchSecrets(secretsMap, config.filter(), null);
        }
        config.filterPrefix().forEach((t, u) -> {
            if (u.enabled()) {
                fetchSecrets(secretsMap, u, t);
            }
        });

        result.add(new SecretsManagerConfigSource(secretsMap, ordinal));

        return result;
    }

    abstract List<SecretListEntry> listSecretsPaginator(Consumer<ListSecretsRequest.Builder> listSecretsRequestBuilder);

    abstract GetSecretValueResponse getSecretValue(
            Consumer<GetSecretValueRequest.Builder> getSecretValueRequestBuilder);

    private void fetchSecrets(Map<String, String> properties, SecretsManagerConfigConfig.FilterConfig filters,
            String prefix) {

        Map<String, String> secretsMap = listSecretsPaginator(b -> {
            List<Filter> requestFilters = new ArrayList<>();
            filters.namePrefix().ifPresent(namePrefix -> {
                requestFilters.add(Filter.builder().key(FilterNameStringType.NAME).values(namePrefix).build());
            });

            filters.primaryRegionPrefix().ifPresent(primaryRegionPrefix -> {
                requestFilters.add(Filter.builder().key(FilterNameStringType.PRIMARY_REGION)
                        .values(primaryRegionPrefix).build());
            });
            filters.owningServicePrefix().ifPresent(owningServicePrefix -> {
                requestFilters.add(Filter.builder().key(FilterNameStringType.OWNING_SERVICE)
                        .values(owningServicePrefix).build());
            });
            filters.tagKey().ifPresent(tagKey -> {
                requestFilters.add(Filter.builder().key(FilterNameStringType.TAG_KEY).values(tagKey).build());
            });
            filters.tagValue().ifPresent(tagValue -> {
                requestFilters
                        .add(Filter.builder().key(FilterNameStringType.TAG_VALUE).values(tagValue).build());
            });
            filters.all().ifPresent(all -> {
                requestFilters
                        .add(Filter.builder().key(FilterNameStringType.ALL).values(all).build());
            });
            filters.description().ifPresent(description -> {
                requestFilters
                        .add(Filter.builder().key(FilterNameStringType.DESCRIPTION).values(description).build());
            });

            b.filters(requestFilters);
        }).stream()
                .collect(Collectors.toUnmodifiableMap(this::getSecretKey,
                        this::getSecretValue));

        properties.putAll(prefixMap(secretsMap, prefix));
    }

    private String getSecretKey(SecretListEntry secret) {
        return secret.name();
    }

    private String getSecretValue(SecretListEntry secret) {
        return getSecretValue(b -> b.secretId(secret.arn())).secretString();
    }

    private Map<String, String> prefixMap(Map<String, String> map, String prefix) {
        return prefix == null
                ? map
                : map.entrySet().stream().collect(toMap(entry -> prefix + "." + entry.getKey(), Map.Entry::getValue));
    }

    private static final class SecretsManagerConfigSource extends MapBackedConfigSource {

        public SecretsManagerConfigSource(Map<String, String> propertyMap, int ordinal) {
            super("SecretsManagerConfigSource", propertyMap, ordinal);
        }
    }
}
