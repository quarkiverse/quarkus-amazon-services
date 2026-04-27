package io.quarkiverse.amazon.common.deployment.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Abstract base processor for dev services.
 * <p>
 * Service processors extend this class to provide dev services support and emit
 * {@link DevServicesAwsStackProviderBuildItem} instances, which are consumed by
 * a central implementation of {@link AbstractDevServicesAwsStackProcessor}.
 * </p>
 */
public abstract class AbstractDevServicesAwsStackProcessor {

    private static final Logger log = Logger.getLogger(AbstractDevServicesAwsStackProcessor.class);

    private static final String ENDPOINT_OVERRIDE = "quarkus.%s.endpoint-override";
    private static final String AWS_REGION = "quarkus.%s.aws.region";
    private static final String AWS_CREDENTIALS_TYPE = "quarkus.%s.aws.credentials.type";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID = "quarkus.%s.aws.credentials.static-provider.access-key-id";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY = "quarkus.%s.aws.credentials.static-provider.secret-access-key";

    /**
     * Setup method to be called by service processors.
     *
     * @param serviceName the name of the AWS service (e.g., "s3", "sqs")
     * @param devServicesBuildTimeConfig the service-specific devservices config
     * @param globalConfig the global AWS devservices config
     * @return DevServicesAwsStackProviderBuildItem or null if dev services should not be started for this service
     */
    protected DevServicesAwsStackProviderBuildItem setup(
            String serviceName,
            DevServicesBuildTimeConfig devServicesBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {

        // explicitly disabled
        if (!devServicesBuildTimeConfig.enabled().orElse(true)) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - %s, as it has been disabled in the config.",
                    serviceName);
            return null;
        }

        String endpointOverride = String.format(ENDPOINT_OVERRIDE, serviceName);
        if (ConfigUtils.isPropertyPresent(endpointOverride)) {
            log.debugf("Not starting Dev Services for Amazon Services - %s, the %s is configured.",
                    serviceName,
                    endpointOverride);
            return null;
        }

        return new DevServicesAwsStackProviderBuildItem(
                serviceName,
                devServicesBuildTimeConfig,
                new DevServicesAwsStackExtensionProvider() {
                    @Override
                    public void prepareAwsStackContainer(AwsStackContainer awsStack) {
                        AbstractDevServicesAwsStackProcessor.this.prepareAwsStackContainer(
                                devServicesBuildTimeConfig,
                                awsStack);
                    }

                    @Override
                    public void reuseAwsStackContainer(AwsStackContainer awsStack) {
                    }

                    @Override
                    public Map<String, Function<AwsStackContainer, String>> getClientConfig() {
                        Map<String, Function<AwsStackContainer, String>> clientConfig = getAwsStackClientConfig(serviceName,
                                null);
                        overrideAwsStackClientConfig(clientConfig);
                        return clientConfig;
                    }
                });
    }

    /**
     * Prepare the stack container before it is started.
     * <p>
     * This method is called before the container starts and allows service-specific
     * initialization (e.g., creating S3 buckets, DynamoDB tables).
     * Default implementation does nothing.
     * </p>
     *
     * @param devServicesBuildTimeConfig build time configuration
     * @param awsStack the stack container object
     */
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig devServicesBuildTimeConfig,
            AwsStackContainer awsStack) {
        // Override in subclasses if needed
    }

    /**
     * Provides the base AWS SDK client configuration properties.
     * <p>
     * Subclasses can override {@link #overrideAwsStackClientConfig(Map)} to customize
     * these properties.
     * </p>
     *
     * @param serviceName the service name for configuration prefix
     * @return map of configuration properties
     */
    protected Map<String, Function<AwsStackContainer, String>> getAwsStackClientConfig(String serviceName,
            AwsStackContainer awsStack) {
        Map<String, Function<AwsStackContainer, String>> config = new HashMap<>();
        config.put(String.format(ENDPOINT_OVERRIDE, serviceName),
                awsStackContainer -> awsStackContainer.getEndpoint().toString());
        config.put(String.format(AWS_REGION, serviceName), AwsStackContainer::getRegion);
        config.put(String.format(AWS_CREDENTIALS_TYPE, serviceName), awsStackContainer -> "static");
        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID, serviceName),
                AwsStackContainer::getAccessKey);
        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY, serviceName),
                AwsStackContainer::getSecretKey);
        return config;
    }

    /**
     * Allows subclasses to override or add additional configuration properties.
     * <p>
     * This method is called after the standard client configuration has been
     * created, allowing subclasses to modify or extend the configuration.
     * Default implementation does nothing.
     * </p>
     *
     * @param config the current configuration map
     */
    protected void overrideAwsStackClientConfig(Map<String, Function<AwsStackContainer, String>> config) {
        // Override in subclasses if needed
    }
}
