package io.quarkiverse.amazon.appconfigdata.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

public class AppConfigDataConfigSourceFactory implements
        ConfigSourceFactory.ConfigurableConfigSourceFactory<AppConfigDataConfigConfig> {

    /**
     * The ordinal is set to < 100 (which is the default) so that this config source is retrieved from last.
     */
    private static final int ORDINAL = 50;

    @Override
    public Iterable<ConfigSource> getConfigSources(
            final ConfigSourceContext configSourceContext,
            final AppConfigDataConfigConfig appConfigDataConfig) {

        if (!appConfigDataConfig.enabled()) {
            return Collections.emptyList();
        }

        var clientConfig = getClientConfig(configSourceContext);

        AppConfigDataClient client = build(clientConfig);
        return Collections.singletonList(new AppConfigDataConfigSource(client,
                appConfigDataConfig.application().orElse(null),
                appConfigDataConfig.environment().orElse(null),
                appConfigDataConfig.configurationProfile().orElse(null),
                appConfigDataConfig.requiredMinimumPollIntervalInSeconds().orElse(null),
                appConfigDataConfig.updateIntervalMinutes(),
                ORDINAL));
    }

    AppConfigDataClient build(AppConfigDataConfig config) throws RuntimeException {
        final AppConfigDataClientBuilder builder = AppConfigDataClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        AmazonClientConfig defaultConfig = config.clients().get(ClientUtil.DEFAULT_CLIENT_NAME);

        AmazonClientCommonRecorder.initAwsClient(builder, "appconfigdata", "appconfigdata", defaultConfig.aws(),
                defaultConfig.aws());
        AmazonClientCommonRecorder.initSdkClientEndpoint(builder, "appconfigdata", "appconfigdata", defaultConfig.sdk(),
                defaultConfig.sdk());

        return builder.build();
    }

    AppConfigDataConfig getClientConfig(ConfigSourceContext context) {

        List<String> profiles = new ArrayList<>(context.getProfiles());
        Collections.reverse(profiles);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withProfiles(profiles)
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withSources(context.getConfigSources())
                .withMapping(AppConfigDataConfig.class)
                .withValidateUnknown(false)
                .build();

        return config.getConfigMapping(AppConfigDataConfig.class);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(ORDINAL);
    }
}
