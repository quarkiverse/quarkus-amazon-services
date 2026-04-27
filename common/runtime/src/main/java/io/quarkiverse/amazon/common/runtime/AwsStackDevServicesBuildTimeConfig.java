package io.quarkiverse.amazon.common.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

/**
 * Base interface for AWS stack dev services build time configuration.
 * Provides common configuration options for different stack implementations.
 */
public interface AwsStackDevServicesBuildTimeConfig {

    /**
     * The container image to use.
     */
    String imageName();

    /**
     * If Dev Services for Amazon Services has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Amazon Services, Dev Services starts a container
     * unless
     * {@code aws.endpoint-override} is set.
     */
    Optional<Boolean> enabled();

    /**
     * Indicates if the stack container managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Amazon Services starts a new container.
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * Path to init scripts folder executed during stack startup.
     */
    Optional<String> initScriptsFolder();

    /**
     * Classpath to init scripts folder executed during stack startup. initScriptsFolder has higher precedence.
     */
    Optional<String> initScriptsClasspath();

    /**
     * Specific container log message to be waiting for stack init scripts completion.
     */
    Optional<String> initCompletionMsg();

    /**
     * Additional services to be started. Use this property if the service
     * you want is not yet covered by the extension.
     * This will provide the required configuration for the service.
     */
    @ConfigDocMapKey("service-name")
    Map<String, DevServicesBuildTimeConfig> additionalServices();

    /**
     * The value of the {@code quarkus-dev-service-{provider}} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for MiniStack looks for a container with the
     * {@code quarkus-dev-service-{provider}} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-{provider}} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared stack containers.
     */
    String serviceName();

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerProperties();

    /**
     * Optional fixed port the stack will listen to.
     */
    Optional<Integer> port();
}