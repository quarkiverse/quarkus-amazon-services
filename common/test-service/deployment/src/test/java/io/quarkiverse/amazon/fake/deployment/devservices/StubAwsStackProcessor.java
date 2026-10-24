package io.quarkiverse.amazon.fake.deployment.devservices;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

/**
 * Plays the role of the central dev services processors without starting any container:
 * the configuration that the services request for a stack is applied to the application.
 */
public class StubAwsStackProcessor {

    public static final String REQUESTED_SERVICES = "test.devservices.requested-services";

    static final AwsStackContainer STUB_CONTAINER = new AwsStackContainer() {
        @Override
        public URI getEndpoint() {
            return URI.create("http://stub-aws-stack:4566");
        }

        @Override
        public String getRegion() {
            return "eu-west-3";
        }

        @Override
        public String getAccessKey() {
            return "stub-access-key";
        }

        @Override
        public String getSecretKey() {
            return "stub-secret-key";
        }
    };

    @BuildStep
    void applyClientConfig(List<DevServicesAwsStackProviderBuildItem> providers,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaults) {
        for (DevServicesAwsStackProviderBuildItem provider : providers) {
            provider.getDevProvider().prepareAwsStackContainer(STUB_CONTAINER);
            provider.getDevProvider().getClientConfig().apply(STUB_CONTAINER)
                    .forEach((key, value) -> runTimeConfigurationDefaults
                            .produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
        }
        runTimeConfigurationDefaults.produce(new RunTimeConfigurationDefaultBuildItem(REQUESTED_SERVICES,
                providers.stream().map(DevServicesAwsStackProviderBuildItem::getServiceName)
                        .collect(Collectors.joining(",", "[", "]"))));
    }
}
