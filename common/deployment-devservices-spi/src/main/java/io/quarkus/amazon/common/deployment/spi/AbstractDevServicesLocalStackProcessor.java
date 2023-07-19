package io.quarkus.amazon.common.deployment.spi;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkus.amazon.common.runtime.AwsCredentialsProviderType;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

public abstract class AbstractDevServicesLocalStackProcessor {

    private static final Logger log = Logger.getLogger(AbstractDevServicesLocalStackProcessor.class);

    private static final String ENDPOINT_OVERRIDE = "quarkus.%s.endpoint-override";
    private static final String AWS_REGION = "quarkus.%s.aws.region";
    private static final String AWS_CREDENTIALS_TYPE = "quarkus.%s.aws.credentials.type";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID = "quarkus.%s.aws.credentials.static-provider.access-key-id";
    private static final String AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY = "quarkus.%s.aws.credentials.static-provider.secret-access-key";

    protected DevServicesLocalStackProviderBuildItem setup(EnabledService enabledService,
            DevServicesBuildTimeConfig devServicesBuildTimeConfig) {

        // explicitely disabled
        if (!devServicesBuildTimeConfig.enabled().orElse(true)) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - %s, as it has been disabled in the config.",
                    enabledService.getName());
            return null;
        }

        String endpointOverride = String.format(ENDPOINT_OVERRIDE, enabledService.getName());
        if (ConfigUtils.isPropertyPresent(endpointOverride)) {
            log.debugf("Not starting Dev Services for Amazon Services - %s, the %s is configured.",
                    enabledService.getName(),
                    endpointOverride);
            return null;
        }

        LocalStackDevServicesBaseConfig sharedConfig = getConfiguration(devServicesBuildTimeConfig);

        return new DevServicesLocalStackProviderBuildItem(enabledService,
                sharedConfig,
                new DevServicesAmazonProvider() {
                    @Override
                    public Map<String, String> prepareLocalStack(LocalStackContainer localstack) {

                        AbstractDevServicesLocalStackProcessor.this.prepareLocalStack(
                                devServicesBuildTimeConfig,
                                localstack);

                        var config = new HashMap<String, String>();
                        config.put(
                                endpointOverride,
                                localstack.getEndpointOverride(enabledService)
                                        .toString());
                        config.put(String.format(AWS_REGION, enabledService.getName()),
                                localstack.getRegion());
                        config.put(String.format(AWS_CREDENTIALS_TYPE,
                                enabledService.getName()),
                                AwsCredentialsProviderType.STATIC.name());
                        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID,
                                enabledService.getName()),
                                localstack.getAccessKey());
                        config.put(String.format(AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY,
                                enabledService.getName()),
                                localstack.getSecretKey());

                        overrideDefaultConfig(config);

                        return config;
                    }

                    @Override
                    public Map<String, String> reuseLocalStack(
                            BorrowedLocalStackContainer localstack) {
                        return Map.of(
                                endpointOverride,
                                localstack.getEndpointOverride(enabledService)
                                        .toString(),
                                String.format(AWS_REGION, enabledService.getName()),
                                localstack.getRegion(),
                                String.format(AWS_CREDENTIALS_TYPE,
                                        enabledService.getName()),
                                AwsCredentialsProviderType.STATIC.name(),
                                String.format(AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID,
                                        enabledService.getName()),
                                localstack.getAccessKey(),
                                String.format(AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY,
                                        enabledService.getName()),
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
            LocalStackContainer localstack) {
    }
}
