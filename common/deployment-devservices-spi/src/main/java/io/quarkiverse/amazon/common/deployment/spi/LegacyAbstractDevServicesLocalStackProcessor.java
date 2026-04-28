package io.quarkiverse.amazon.common.deployment.spi;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.runtime.AwsCredentialsProviderType;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

@Deprecated(forRemoval = true)
public abstract class LegacyAbstractDevServicesLocalStackProcessor {

    private static final Logger log = Logger.getLogger(LegacyAbstractDevServicesLocalStackProcessor.class);

    private static final String ENDPOINT_OVERRIDE = "quarkus.%s.endpoint-override";
    private static final String AWS_REGION = "quarkus.%s.aws.region";
    private static final String AWS_CREDENTIALS_TYPE = "quarkus.%s.aws.credentials.type";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID = "quarkus.%s.aws.credentials.static-provider.access-key-id";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY = "quarkus.%s.aws.credentials.static-provider.secret-access-key";

    protected DevServicesLocalStackProviderBuildItem setup(EnabledService enabledService,
            DevServicesBuildTimeConfig devServicesBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {

        String propertyConfigurationName = getPropertyConfigurationName(enabledService);

        // Check stack configuration - only emit BuildItem if stack is "localstack"
        GlobalDevServicesBuildTimeConfig.AwsStack stack = devServicesBuildTimeConfig.provider().orElse(globalConfig.provider());
        if (stack != GlobalDevServicesBuildTimeConfig.AwsStack.LOCALSTACK) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - %s, stack is configured to '%s' (not 'localstack').",
                    enabledService.getName(), stack);
            return null;
        }

        // explicitly disabled
        if (!devServicesBuildTimeConfig.enabled().orElse(true)) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - %s, as it has been disabled in the config.",
                    enabledService.getName());
            return null;
        }

        String endpointOverride = String.format(ENDPOINT_OVERRIDE, propertyConfigurationName);
        if (ConfigUtils.isPropertyPresent(endpointOverride)) {
            log.debugf("Not starting Dev Services for Amazon Services - %s, the %s is configured.",
                    enabledService.getName(),
                    endpointOverride);
            return null;
        }

        LocalStackDevServicesBaseConfig sharedConfig = getConfiguration(devServicesBuildTimeConfig);

        return new DevServicesLocalStackProviderBuildItem(enabledService,
                sharedConfig,
                new DevServicesLocalStackAmazonProvider() {
                    @Override
                    public Map<String, String> prepareLocalStack(AwsStackContainer localstack) {
                        LegacyAbstractDevServicesLocalStackProcessor.this.prepareLocalStack(
                                devServicesBuildTimeConfig,
                                localstack);

                        var config = new HashMap<String, String>();

                        // Standard configuration - use the container's generated endpoint
                        // The hostname modification for shared network is handled in DevServicesLocalStackProcessor
                        config.put(
                                endpointOverride,
                                localstack.getEndpoint().toString());

                        config.put(String.format(AWS_REGION, propertyConfigurationName),
                                localstack.getRegion());
                        config.put(String.format(AWS_CREDENTIALS_TYPE,
                                propertyConfigurationName),
                                AwsCredentialsProviderType.STATIC.name());
                        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID,
                                propertyConfigurationName),
                                localstack.getAccessKey());
                        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY,
                                propertyConfigurationName),
                                localstack.getSecretKey());

                        overrideDefaultConfig(config);

                        return config;
                    }

                    @Override
                    public Map<String, String> reuseLocalStack(
                            AwsStackContainer localstack) {
                        return Map.of(
                                endpointOverride,
                                localstack.getEndpoint()
                                        .toString(),
                                String.format(AWS_REGION, propertyConfigurationName),
                                localstack.getRegion(),
                                String.format(AWS_CREDENTIALS_TYPE,
                                        propertyConfigurationName),
                                AwsCredentialsProviderType.STATIC.name(),
                                String.format(AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID,
                                        propertyConfigurationName),
                                localstack.getAccessKey(),
                                String.format(AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY,
                                        propertyConfigurationName),
                                localstack.getSecretKey());
                    }
                });
    }

    protected void overrideDefaultConfig(Map<String, String> defaultConfig) {
    }

    /**
     * Returns an equatable configuration
     *
     * @param devServicesBuildTimeConfig build time configuration
     * @return
     */
    protected LocalStackDevServicesBaseConfig getConfiguration(
            DevServicesBuildTimeConfig devServicesBuildTimeConfig) {
        return new LocalStackDevServicesBaseConfig(
                devServicesBuildTimeConfig.shared(),
                devServicesBuildTimeConfig.isolated(),
                devServicesBuildTimeConfig.serviceName(),
                devServicesBuildTimeConfig.containerProperties());
    }

    /**
     * Prepare the owned localStack container
     *
     * @param devServicesBuildTimeConfig build time configuration to apply to container
     * @param localstack the new localStack container to prepare
     */
    protected void prepareLocalStack(DevServicesBuildTimeConfig devServicesBuildTimeConfig,
            AwsStackContainer localstack) {
    }

    /**
     * Returns the property configuration name for the given {@link LocalStackContainer.EnabledService}.
     * <p>
     * The property configuration name is the name of the service, which is the same as the AWSSDK artifact id.
     * The only exception is the Step Functions service, which is named "sfn" in the AWSSDK, "stepfunctions"
     * and "logs" in the LocalStack configuration.
     * <p>
     *
     * @param enabledService the LocalStack enabled service
     * @return the property configuration name
     */
    protected String getPropertyConfigurationName(LocalStackContainer.EnabledService enabledService) {
        if (enabledService == LocalStackContainer.Service.STEPFUNCTIONS)
            return "sfn";
        if (enabledService.getName().equals("events"))
            return "eventbridge";
        if (enabledService == LocalStackContainer.Service.CLOUDWATCHLOGS)
            return "cloudwatchlogs";
        if (enabledService.getName().equals("scheduler"))
            return "eventbridge-scheduler";
        return enabledService.getName();
    }
}
