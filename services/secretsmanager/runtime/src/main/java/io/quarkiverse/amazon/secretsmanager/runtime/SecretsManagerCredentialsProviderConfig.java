package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.secretsmanager")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecretsManagerCredentialsProviderConfig {
    /** Named credential sets for consumers such as {@code quarkus.datasource.credentials-provider=db}. */
    Map<String, CredentialSet> credentialsProvider();

    @ConfigGroup
    interface CredentialSet {
        /** Secret name or ARN containing a credential document. Mutually exclusive with {@code secrets}. */
        Optional<String> secretId();

        /**
         * Document format: json (default), properties, yaml (requires quarkus-config-yaml), or raw (password only).
         * The auto format is not supported for credentials.
         */
        @WithDefault("json")
        ConfigSourceFormat format();

        /** Document field containing the username; an empty setting omits the username. Missing fields are also omitted. */
        @WithDefault("username")
        Optional<String> usernameKey();

        /** Required password field in a JSON, properties or YAML document. */
        @WithDefault("password")
        String passwordKey();

        /** Individual raw secrets keyed by Quarkus credential name: password (required) and user (optional). */
        Map<String, String> secrets();

        /** Use BatchGetSecretValue for the explicit selection, requiring its additional IAM permission. No fallback. */
        @WithDefault("false")
        boolean batchRetrieval();
    }
}
