package io.quarkiverse.amazon.rds.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.rds")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RdsCredentialsProviderConfig {

    /**
     * List of named credentials providers, such as: `quarkus.rds.credentials-provider.foo.username=myuser`
     * <p>
     * This defines a credentials provider `foo` returning key `password` from RDS.
     * Once defined, this provider can be used in credentials consumers, such as the Agroal connection pool.
     * <p>
     * Example: `quarkus.datasource.credentials-provider=foo`
     *
     * @asciidoclet
     */
    Map<String, CredentialsProviderConfig> credentialsProvider();

    public interface CredentialsProviderConfig {
        /**
         * The username to use for the RDS IAM authentication.
         */
        String username();

        /**
         * The hostname of the RDS instance.
         */
        String hostname();

        /**
         * The port of the RDS instance.
         */
        int port();
    }
}
