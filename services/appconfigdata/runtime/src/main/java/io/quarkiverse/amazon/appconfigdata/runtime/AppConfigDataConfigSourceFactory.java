package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataConfigSource.Selection;
import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
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
     * The ordinal is set to 270 so that this config source is retrieved after .env and before application.properties files.
     * This is also aligned to other quarkus extension config sources.
     */
    private static final int ORDINAL = 270;

    @Override
    public Iterable<ConfigSource> getConfigSources(
            final ConfigSourceContext configSourceContext,
            final AppConfigDataConfigConfig appConfigDataConfig) {

        if (!appConfigDataConfig.enabled()) {
            return Collections.emptyList();
        }

        appConfigDataConfig.format().checkAvailable();
        Duration interval = appConfigDataConfig.updateIntervalMinutes() != 0
                ? Duration.ofMinutes(appConfigDataConfig.updateIntervalMinutes())
                : appConfigDataConfig.updateInterval();
        AbstractAwsConfigSource.validateUpdateInterval(interval);

        var selections = selections(appConfigDataConfig);
        if (selections.isEmpty()) {
            return Collections.emptyList();
        }
        selections.forEach(selection -> selection.format().checkAvailable());
        var clientConfig = getClientConfig(configSourceContext);

        AppConfigDataClient client = build(clientConfig);

        return Collections.singletonList(
                new AppConfigDataConfigSource(client, selections, interval, appConfigDataConfig.ordinal(),
                        appConfigDataConfig.failOnMissingConfig()));
    }

    static List<Selection> selections(AppConfigDataConfigConfig config) {
        var selections = new ArrayList<Selection>();
        boolean legacy = config.application().isPresent() || config.environment().isPresent()
                || config.configurationProfile().isPresent();
        if (legacy || config.entries().isEmpty()) {
            var profiles = AppConfigDataConfigSource.profiles(config.application().orElse(null),
                    config.environment().orElse(null),
                    config.configurationProfile().orElse(null), config.requiredMinimumPollIntervalInSeconds().orElse(null));
            if (profiles.isEmpty() && config.failOnMissingConfig()) {
                throw new IllegalArgumentException(
                        "AppConfig Data ConfigSource requires application, environment and configuration-profile.");
            }
            profiles.forEach(profile -> selections.add(new Selection("legacy", profile, "", config.format())));
        }
        config.entries().forEach((name, entry) -> selections.add(new Selection("entries:" + name,
                new Profile(entry.application(), entry.environment(), entry.configurationProfile(),
                        entry.requiredMinimumPollIntervalInSeconds().or(config::requiredMinimumPollIntervalInSeconds)
                                .orElse(null)),
                entry.prefix().orElse(""), entry.format().orElse(config.format()))));
        return List.copyOf(selections);
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
