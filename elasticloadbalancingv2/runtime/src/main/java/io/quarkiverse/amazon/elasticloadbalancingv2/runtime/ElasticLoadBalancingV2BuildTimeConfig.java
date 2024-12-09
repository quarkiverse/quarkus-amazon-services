package io.quarkiverse.amazon.elasticloadbalancingv2.runtime;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Elastic Load Balancer v2 build time configuration
 */
@ConfigMapping(prefix = "quarkus.elasticloadbalancingv2")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ElasticLoadBalancingV2BuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
