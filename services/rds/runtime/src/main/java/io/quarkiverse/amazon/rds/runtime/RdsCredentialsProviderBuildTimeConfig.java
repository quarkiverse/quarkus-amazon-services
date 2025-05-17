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
     * List of named credentials providers, such as: `quarkus.rds.credentials-provider.foo.use-quarkus-client=true`
     * <p>
     * This defines a rds credentials provider that can use a quarkus configured rds client.
     * <p>
     *
     * @asciidoclet
     */
    Map<String, CredentialsProviderBuildTimeConfig> credentialsProvider();

    public interface CredentialsProviderBuildTimeConfig {
        /**
         * Enables the named credentials provider to use a quarkus rds client, otherwise a default rds client will be created.
         */
        boolean useQuarkusClient();

        /**
         * The name of the quarkus rds client to use.
         */
        Optional<String> name();
    }
}
