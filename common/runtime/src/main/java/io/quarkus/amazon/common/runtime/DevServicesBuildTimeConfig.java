package io.quarkus.amazon.common.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesBuildTimeConfig {

    /**
     * If local stack based dev services should be used. This requires expliect config rather than
     * being activated by a lack of config for dev services, as AWS is a singleton and does not seen
     * to be configured in the same way as other services.
     *
     * If this is true then a localstack S3 container will be started and will be used instead of AWS.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Indicates if the localstack managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Kafka starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-localstack-s3} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-localstack-s3} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for S3 looks for a container with the
     * {@code quarkus-dev-service-localstack-s3} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-localstack-s3} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared S3 instances.
     */
    @ConfigItem(defaultValue = "localstack")
    public String serviceName;

    /**
     * Generic properties that are pass for additional container configuration.
     */
    @ConfigItem
    public Map<String, String> containerProperties;
}
