package io.quarkiverse.amazon.s3.runtime;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.runtime.AwsConfigSourceClient;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.s3.runtime.S3ConfigProvider.ObjectLocation;
import io.quarkiverse.amazon.s3.runtime.S3ConfigSource.Selection;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

public class S3ConfigSourceFactory implements ConfigSourceFactory.ConfigurableConfigSourceFactory<S3ConfigSourceConfig> {
    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, S3ConfigSourceConfig config) {
        if (!config.enabled()) {
            return Collections.emptyList();
        }
        long maxSize = config.maxObjectSize().asLongValue();
        if (maxSize <= 0 || maxSize >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("quarkus.s3.config.max-object-size must be positive and less than 2 GiB.");
        }
        var interval = config.updateInterval();
        AbstractAwsConfigSource.validateUpdateInterval(interval);
        var entries = config.entries().entrySet().stream().map(entry -> {
            var object = entry.getValue();
            var format = object.format().orElse(config.format());
            format.checkAvailable();
            return new Selection("entries:" + entry.getKey(),
                    new ObjectLocation(object.bucket(), object.key(), object.versionId().orElse(null)),
                    object.prefix().orElse(""), format);
        }).toList();
        if (entries.isEmpty()) {
            if (config.failOnMissingConfig()) {
                throw new IllegalArgumentException(
                        "S3 ConfigSource requires explicit entries; bucket listing is not supported.");
            }
            Logger.getLogger(S3ConfigSourceFactory.class).warn("S3 ConfigSource has no configured entries; skipping it.");
            return Collections.emptyList();
        }
        var client = build(getClientConfig(context), context);
        return List.of(
                new S3ConfigSource(client, entries, (int) maxSize, interval, config.ordinal(), config.failOnMissingConfig()));
    }

    S3Client build(S3Config config, ConfigSourceContext context) {
        var builder = (S3ClientBuilder) new S3Recorder(new RuntimeValue<>(config)).getSyncClientBuilder();
        AwsConfigSourceClient.configure(builder, "s3", config.clients().get(ClientUtil.DEFAULT_CLIENT_NAME),
                config.syncClient(), context);
        return builder.build();
    }

    S3Config getClientConfig(ConfigSourceContext context) {
        return AwsConfigSourceClient.configBuilder(context).withMapping(S3Config.class).build()
                .getConfigMapping(S3Config.class);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(270);
    }
}
