package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public class SecretsManagerConfigSourceFactory implements
        ConfigSourceFactory.ConfigurableConfigSourceFactory<SecretsManagerConfigConfig> {

    /**
     * The ordinal is set to 270 so that this config source is retrieved after .env and before application.properties files.
     * This is also aligned to other quarkus extension config sources.
     */
    private static final int ORDINAL = 270;

    @Override
    public Iterable<ConfigSource> getConfigSources(
            final ConfigSourceContext configSourceContext,
            final SecretsManagerConfigConfig secretsManagerConfig) {

        if (!secretsManagerConfig.enabled()) {
            return Collections.emptyList();
        }

        var interval = secretsManagerConfig.updateInterval();
        AbstractAwsConfigSource.validateUpdateInterval(interval);

        secretsManagerConfig.format().checkAvailable();
        var entries = entries(secretsManagerConfig);
        if (entries.isEmpty()) {
            if (secretsManagerConfig.failOnMissingConfig()) {
                throw new IllegalArgumentException(
                        "Secrets Manager ConfigSource requires entries or secrets; implicit listing is not supported.");
            }
            Logger.getLogger(SecretsManagerConfigSourceFactory.class)
                    .warn("Secrets Manager ConfigSource has no configured entries or secrets; skipping it.");
            return Collections.emptyList();
        }
        entries.forEach(entry -> entry.format().checkAvailable());
        var clientConfig = getClientConfig(configSourceContext);

        SecretsManagerClient client = build(clientConfig);
        return Collections
                .singletonList(new SecretsManagerConfigSource(client, entries, secretsManagerConfig.ordinal(),
                        secretsManagerConfig.failOnMissingConfig(), interval));
    }

    static List<ConfigSourceEntry> entries(SecretsManagerConfigConfig config) {
        var entries = new ArrayList<ConfigSourceEntry>();
        config.secrets().forEach((prefix, secret) -> entries.add(
                new ConfigSourceEntry("secrets:" + prefix, new ResourceId(secret), prefix, config.format(), false)));
        config.entries().forEach((name, entry) -> entries.add(new ConfigSourceEntry("entries:" + name,
                new ResourceId(entry.secretId()), entry.prefix().orElse(""), entry.format().orElse(config.format()), false)));
        return List.copyOf(entries);
    }

    SecretsManagerClient build(SecretsManagerConfig config) throws RuntimeException {
        final SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        AmazonClientConfig defaultConfig = config.clients().get(ClientUtil.DEFAULT_CLIENT_NAME);

        AmazonClientCommonRecorder.initAwsClient(builder, "secretsmanager", "secretsmanager", defaultConfig.aws(),
                defaultConfig.aws());
        AmazonClientCommonRecorder.initSdkClientEndpoint(builder, "secretsmanager", "secretsmanager", defaultConfig.sdk(),
                defaultConfig.sdk());

        return builder.build();
    }

    SecretsManagerConfig getClientConfig(ConfigSourceContext context) {

        List<String> profiles = new ArrayList<>(context.getProfiles());
        Collections.reverse(profiles);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withProfiles(profiles)
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withSources(context.getConfigSources())
                .withMapping(SecretsManagerConfig.class)
                .withValidateUnknown(false)
                .build();

        return config.getConfigMapping(SecretsManagerConfig.class);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(ORDINAL);
    }
}
