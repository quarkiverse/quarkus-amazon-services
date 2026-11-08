package io.quarkiverse.amazon.common.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.config.SmallRyeConfig;

/** Close all AWS sources of the current application/classloader on shutdown, including development-mode restarts. */
@ApplicationScoped
public class AwsConfigSourceCloser {
    private static final Logger LOG = Logger.getLogger(AwsConfigSourceCloser.class);

    void onStop(@Observes ShutdownEvent event) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (ConfigSource source : config.getConfigSources(AbstractAwsConfigSource.class)) {
            try {
                ((AbstractAwsConfigSource) source).close();
            } catch (Exception failure) {
                LOG.errorf("Unable to close AWS configuration source %s [%s]", source.getName(),
                        failure.getClass().getSimpleName());
            }
        }
        for (ConfigSource source : config.getConfigSources(AwsConfigExpression.class)) {
            try {
                ((AwsConfigExpression) source).close();
            } catch (Exception failure) {
                LOG.errorf("Unable to close AWS expression handler %s [%s]", source.getName(),
                        failure.getClass().getSimpleName());
            }
        }
    }
}
