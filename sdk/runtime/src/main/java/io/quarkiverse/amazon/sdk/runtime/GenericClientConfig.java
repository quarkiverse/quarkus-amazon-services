package io.quarkiverse.amazon.sdk.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.AwsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface GenericClientConfig extends AmazonClientConfig {
    /** Region and credentials, without an additional aws segment. */
    @Override
    @WithParentName
    AwsConfig aws();
}
