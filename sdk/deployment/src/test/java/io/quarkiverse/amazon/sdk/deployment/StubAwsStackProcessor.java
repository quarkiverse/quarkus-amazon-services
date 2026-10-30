package io.quarkiverse.amazon.sdk.deployment;

import java.net.URI;
import java.util.List;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class StubAwsStackProcessor {
    static final AwsStackContainer STACK = new AwsStackContainer() {
        @Override
        public URI getEndpoint() {
            return URI.create("http://stub-aws:4566");
        }

        @Override
        public String getRegion() {
            return "eu-west-3";
        }

        @Override
        public String getAccessKey() {
            return "stub-key";
        }

        @Override
        public String getSecretKey() {
            return "stub-secret";
        }
    };

    @BuildStep
    void defaults(List<DevServicesAwsStackProviderBuildItem> providers,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaults) {
        for (DevServicesAwsStackProviderBuildItem provider : providers) {
            provider.getDevProvider().getClientConfig().apply(STACK)
                    .forEach((key, value) -> defaults.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
        }
        defaults.produce(new RunTimeConfigurationDefaultBuildItem("test.aws.requested-services",
                providers.stream().map(DevServicesAwsStackProviderBuildItem::getServiceName).sorted().toList().toString()));
    }
}
