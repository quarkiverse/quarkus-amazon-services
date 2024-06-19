package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface AmazonClientConfig {
    /**
     * AWS SDK client configurations
     */
    @WithParentName
    @ConfigDocSection
    SdkConfig sdk();

    /**
     * AWS services configurations
     */
    @ConfigDocSection
    AwsConfig aws();
}
