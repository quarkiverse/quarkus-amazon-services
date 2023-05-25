package io.quarkus.amazon.dynamodb.enhanced.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "dynamodbenhanced", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DynamoDbEnhancedBuildTimeConfig {

    /**
     * List of extensions to load with the enhanced client.
     * <p>
     * The list should consists of class names which implements
     * {@code software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension} interface.
     *
     * @see software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
     */
    @ConfigItem
    @ConfigDocSection
    public Optional<List<String>> clientExtensions;

    /**
     * Whether a {@link TableSchema} should be created at
     * start up classes that has been annotated with DynamoDb enhanced client annotations
     * <p>
     * {@link TableSchema} are cached and can be retrieved later with {@code TableSchema.fromClass()}
     */
    @ConfigItem(defaultValue = "true")
    public boolean createTableSchemas;
}
