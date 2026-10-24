package io.quarkiverse.amazon.ssm.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.config.SmallRyeConfig;

/**
 * Neither SmallRye Config nor Quarkus closes config sources, so the poller and client of a {@link SsmConfigSource}
 * would outlive the application, and pile up on each dev mode restart. This closes them on shutdown.
 * <p>
 * Only the sources of the {@link SmallRyeConfig} registered for the current classloader are found.
 */
@ApplicationScoped
public class SsmConfigSourceCloser {

    void onStop(@Observes ShutdownEvent event) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (ConfigSource configSource : config.getConfigSources(SsmConfigSource.class)) {
            ((SsmConfigSource) configSource).close();
        }
    }
}
