package io.quarkiverse.amazon.ses.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SesDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The email addresses to verify on startup.
     */
    Optional<List<String>> verifiedEmails();

}
