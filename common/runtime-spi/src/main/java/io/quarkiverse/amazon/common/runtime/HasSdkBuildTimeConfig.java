package io.quarkiverse.amazon.common.runtime;

import io.smallrye.config.WithParentName;

public interface HasSdkBuildTimeConfig {

    /** Configuration prefix used in diagnostics, independently of the service identifier. */
    default String configPrefix(String extension) {
        return extension;
    }

    /**
     * SDK client configurations for AWS client
     */
    @WithParentName
    SdkBuildTimeConfig sdk();
}
