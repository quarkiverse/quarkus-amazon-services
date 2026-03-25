package io.quarkiverse.amazon.secretsmanager.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public class SecretManagerConfigSourceFactory implements
        ConfigSourceFactory.ConfigurableConfigSourceFactory<SecretManagerConfigConfig> {

    /**
     * The ordinal is set to < 100 (which is the default) so that this config source is retrieved from last.
     */
    private static final int ORDINAL = 50;

    @Override
    public Iterable<ConfigSource> getConfigSources(
            final ConfigSourceContext configSourceContext,
            final SecretManagerConfigConfig secretsManagerConfig) {

        if (!secretsManagerConfig.enabled()) {
            return Collections.emptyList();
        }

        try (SecretsManagerClient client = build(secretsManagerConfig)) {
            Map<String, String> lookup = secretsManagerConfig.secrets();
            return Collections.singletonList(new SecretManagerConfigSource(client, lookup, ORDINAL));
        }
    }

    SecretsManagerClient build(SecretManagerConfigConfig config) throws RuntimeException {
        final SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        final Config microprofileConfig = ConfigProvider.getConfig();
        final Optional<URI> endpoint = microprofileConfig.getOptionalValue(
                "quarkus.secretsmanager.endpoint-override",
                URI.class);
        final Optional<Region> region = microprofileConfig.getOptionalValue(
                "quarkus.secretsmanager.aws.region",
                Region.class);
        final Optional<String> accessKey = microprofileConfig.getOptionalValue(
                "quarkus.secretsmanager.credentials.static-provider.access-key-id",
                String.class);
        final Optional<String> secretAccessKey = microprofileConfig.getOptionalValue(
                "quarkus.secretsmanager.credentials.static-provider.secret-access-key",
                String.class);

        endpoint.ifPresent(builder::endpointOverride);
        region.ifPresent(builder::region);

        if (accessKey.isPresent() && secretAccessKey.isPresent()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                    .create(accessKey.get(), secretAccessKey.get())));
        }

        return builder.build();
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(ORDINAL);
    }

}
