package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

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
     * The ordinal is set to < 100 (which is the default) so that this config source is retrieved from last.
     */
    private static final int ORDINAL = 50;

    @Override
    public Iterable<ConfigSource> getConfigSources(
            final ConfigSourceContext configSourceContext,
            final SecretsManagerConfigConfig secretsManagerConfig) {

        if (!secretsManagerConfig.enabled()) {
            return Collections.emptyList();
        }

        var clientConfig = getClientConfig(configSourceContext);

        try (SecretsManagerClient client = build(clientConfig)) {
            Map<String, String> lookup = secretsManagerConfig.secrets();
            return Collections.singletonList(new SecretsManagerConfigSource(client, lookup, ORDINAL));
        }
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
