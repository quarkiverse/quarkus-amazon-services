package io.quarkiverse.amazon.sdk.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig.AwsStack;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.aws")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface AmazonSdkBuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {
    @Override
    default String configPrefix(String extension) {
        return "aws";
    }

    /** Default Dev Services settings for generic clients. */
    DevServicesBuildTimeConfig devservices();

    /** Named client Dev Services overrides. */
    @WithParentName
    @ConfigDocMapKey("client-name")
    Map<String, NamedClientBuildTimeConfig> clients();

    @ConfigGroup
    interface NamedClientBuildTimeConfig {
        /** Dev Services settings for this client. */
        NamedDevServicesConfig devservices();
    }

    @ConfigGroup
    interface NamedDevServicesConfig {
        /** Stack provider for this client. If unset, inherits the global provider. */
        Optional<AwsStack> provider();
    }
}
