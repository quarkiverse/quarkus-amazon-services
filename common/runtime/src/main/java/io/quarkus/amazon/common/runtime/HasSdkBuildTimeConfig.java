package io.quarkus.amazon.common.runtime;

import io.smallrye.config.WithParentName;

public interface HasSdkBuildTimeConfig {

    /**
     * SDK client configurations for AWS client
     */
    @WithParentName
    SdkBuildTimeConfig sdk();
}
