package io.quarkiverse.amazon.sesv2.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SesV2DevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The email addresses to verify on startup, so that they can be used as sender addresses.
     */
    Optional<List<String>> verifiedEmails();

}
