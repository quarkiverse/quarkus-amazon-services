package io.quarkiverse.amazon.common.runtime;

import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;

/**
 * Registers the capturing interceptor when the client is built.
 * <p>
 * Plugins are kept apart from the override configuration, so the interceptor survives an application replacing it, e.g.
 * with {@code builder.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))}.
 */
final class VertxContextPropagationPlugin implements SdkPlugin {

    private final VertxContextCapturingInterceptor interceptor;

    VertxContextPropagationPlugin(VertxContextCapturingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        config.overrideConfiguration(config.overrideConfiguration().toBuilder()
                .addExecutionInterceptor(interceptor)
                .build());
    }
}
