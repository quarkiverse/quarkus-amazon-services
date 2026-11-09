package io.quarkiverse.amazon.dynamodb.enhanced.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@ConfigMapping(prefix = "quarkus.dynamodbenhanced")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DynamoDbEnhancedBuildTimeConfig {

    /**
     * Enhanced client configuration. Named clients inherit absent settings from the default client.
     */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(ClientUtil.DEFAULT_CLIENT_NAME)
    Map<String, ClientConfig> clients();

    default Optional<List<String>> clientExtensions() {
        return clientExtensions(ClientUtil.DEFAULT_CLIENT_NAME);
    }

    default Optional<List<String>> clientExtensions(String clientName) {
        ClientConfig defaults = clients().get(ClientUtil.DEFAULT_CLIENT_NAME);
        return clients().getOrDefault(clientName, defaults).clientExtensions().or(defaults::clientExtensions);
    }

    @ConfigGroup
    interface ClientConfig {
        /**
         * List of extensions to load with the enhanced client. A named client's list replaces the default client's list.
         * <p>
         * The list should consist of class names which implement
         * {@code software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension}.
         *
         * @see software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
         */
        @ConfigDocSection
        Optional<List<String>> clientExtensions();
    }

    /**
     * Whether a {@link TableSchema} should be created at start up for DynamoDb mappable entities
     * annotated with {@link DynamoDbBean} or {@link DynamoDbImmutable}
     * <p>
     * {@link TableSchema} are cached and can be retrieved later with {@code TableSchema.fromClass()}
     */
    @WithDefault(value = "true")
    boolean createTableSchemas();
}
