package io.quarkiverse.amazon.ssm.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.AwsConfigSourceClient;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.ssm.runtime.SsmConfigProvider.Path;
import io.quarkiverse.amazon.ssm.runtime.SsmConfigSource.Selection;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public class SsmConfigSourceFactory implements ConfigSourceFactory.ConfigurableConfigSourceFactory<SsmConfigConfig> {

    private static final Logger LOG = Logger.getLogger(SsmConfigSourceFactory.class);

    /**
     * The ordinal is set to 270 so that this config source is retrieved after .env and before application.properties files.
     * This is also aligned to other quarkus extension config sources.
     */
    private static final int ORDINAL = 270;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext, SsmConfigConfig ssmConfig) {

        if (!ssmConfig.enabled()) {
            return Collections.emptyList();
        }

        Duration interval = ssmConfig.updateIntervalMinutes() != 0 ? Duration.ofMinutes(ssmConfig.updateIntervalMinutes())
                : ssmConfig.updateInterval();
        AbstractAwsConfigSource.validateUpdateInterval(interval);

        var paths = selections(ssmConfig);
        List<String> names = sanitizeNames(ssmConfig.names().orElse(List.of()));

        if (paths.isEmpty() && names.isEmpty()) {
            if (ssmConfig.failOnMissingConfig()) {
                throw new IllegalArgumentException(
                        "SSM ConfigSource requires entries, path or names.");
            }
            LOG.error(
                    "SSM ConfigSource is enabled but no entries, path or names are configured.");
            return Collections.emptyList();
        }

        ssmConfig.format().checkAvailable();
        paths.forEach(selection -> selection.format().checkAvailable());
        SsmConfig clientConfig = getClientConfig(configSourceContext);
        SsmClient client = build(clientConfig, configSourceContext);

        return Collections.singletonList(new SsmConfigSource(client, paths, names, ssmConfig.withDecryption(), interval,
                ssmConfig.ordinal(), ssmConfig.format(), ssmConfig.keyNaming(), ssmConfig.failOnMissingConfig()));
    }

    static List<Selection> selections(SsmConfigConfig config) {
        var paths = new ArrayList<Selection>();
        config.path().filter(p -> !isBlank(p)).ifPresent(path -> paths.add(
                new Selection("path", new Path(path, config.recursive()), "", config.format())));
        config.prefixedPaths().forEach((prefix, path) -> paths.add(
                new Selection("path:" + prefix, new Path(path, config.recursive()), prefix, config.format())));
        config.entries().forEach((name, entry) -> paths.add(new Selection("entries:" + name,
                new Path(entry.path(), entry.recursive().orElse(config.recursive())), entry.prefix().orElse(""),
                entry.format().orElse(config.format()))));
        return List.copyOf(paths);
    }

    private static List<String> sanitizeNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (n != null && !isBlank(n)) {
                out.add(n.trim());
            }
        }
        return List.copyOf(out);
    }

    SsmClient build(SsmConfig config, ConfigSourceContext context) {
        SsmClientBuilder builder = SsmClient.builder();
        AmazonClientConfig defaultConfig = config.clients().get(ClientUtil.DEFAULT_CLIENT_NAME);
        AwsConfigSourceClient.configure(builder, "ssm", defaultConfig, config.syncClient(), context);
        return builder.build();
    }

    SsmConfig getClientConfig(ConfigSourceContext context) {
        return AwsConfigSourceClient.configBuilder(context).withMapping(SsmConfig.class).build()
                .getConfigMapping(SsmConfig.class);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(ORDINAL);
    }
}
