package io.quarkiverse.amazon.rds.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.rds")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface RdsCredentialsProviderBuildTimeConfig {

    /**
     * List of named credentials providers, such as: `quarkus.rds.credentials-provider.foo.name=iam`
     * <p>
     * This configures how a rds credentials provider gets the region and credentials used to sign its tokens.
     * <p>
     *
     * @asciidoclet
     */
    Map<String, CredentialsProviderBuildTimeConfig> credentialsProvider();

    public interface CredentialsProviderBuildTimeConfig {
        /**
         * The name of the quarkus rds client to use. If not set, the default client is used.
         */
        Optional<String> name();
    }
}
