package io.quarkus.amazon.dynamodb.enhanced.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@ConfigMapping(prefix = "quarkus.dynamodbenhanced")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DynamoDbEnhancedBuildTimeConfig {

    /**
     * List of extensions to load with the enhanced client.
     * <p>
     * The list should consists of class names which implements
     * {@code software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension} interface.
     *
     * @see software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
     */
    @ConfigDocSection
    Optional<List<String>> clientExtensions();

    /**
     * Whether a {@link TableSchema} should be created at start up for DynamoDb mappable entities
     * annotated with {@link DynamoDbBean} or {@link DynamoDbImmutable}
     * <p>
     * {@link TableSchema} are cached and can be retrieved later with {@code TableSchema.fromClass()}
     */
    @WithDefault(value = "true")
    boolean createTableSchemas();
}
