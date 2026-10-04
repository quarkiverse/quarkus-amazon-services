package io.quarkiverse.amazon.common.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DevServicesBuildTimeConfig {

    /**
     * If a local AWS stack should be used. (default to true)
     *
     * If this is true and endpoint-override is not configured then a local AWS stack
     * will be started and will be used instead of the given configuration.
     * For all services but Cognito, the local AWS stack will be provided by LocalStack.
     * Otherwise, it will be provided by Moto
     */
    Optional<Boolean> enabled();

    /**
     * Generic properties that are pass for additional container configuration.
     */
    Map<String, String> containerProperties();

    /**
     * The dev services stack provider to use for this service.
     * If not specified, the global setting from quarkus.aws.devservices.provider is used.
     * Supported values: "localstack", "ministack"
     * <p>
     * This allows per-service override of the global stack selection.
     * </p>
     */
    Optional<GlobalDevServicesBuildTimeConfig.AwsStack> provider();
}
