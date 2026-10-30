package io.quarkiverse.amazon.common.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.FlociDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class DevServicesConfigurationTest {
    private static AbstractDevServicesAwsStackProcessor.ServiceConfig serviceConfig(Map<String, String> properties,
            String... services) {
        var config = new SmallRyeConfigBuilder()
                .withMapping(GlobalDevServicesBuildTimeConfig.class)
                .withMapping(FlociDevServicesBuildTimeConfig.class)
                .withMapping(ServiceConfig.class)
                .withValidateUnknown(false)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build();
        return AbstractDevServicesAwsStackProcessor.buildServiceConfig(
                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class),
                config.getConfigMapping(FlociDevServicesBuildTimeConfig.class),
                Arrays.stream(services).map(service -> new DevServicesAwsStackProviderBuildItem(service,
                        config.getConfigMapping(ServiceConfig.class).devservices(), null)).toList());
    }

    @Test
    void ignoresClientTuningButRetainsGlobalAndDedicatedProvisioningSettings() {
        Map<String, String> settings = Map.of("quarkus.aws.devservices.floci.image-name", "floci/floci:latest",
                "quarkus.s3.devservices.buckets", "example",
                "quarkus.s3.api-call-timeout", "10s");
        var first = serviceConfig(settings, "s3");
        assertEquals(first, serviceConfig(Map.of("quarkus.aws.devservices.floci.image-name", "floci/floci:latest",
                "quarkus.s3.devservices.buckets", "example", "quarkus.s3.api-call-timeout", "20s"), "s3"));
        assertNotEquals(first, serviceConfig(Map.of("quarkus.aws.devservices.floci.image-name", "floci/floci:latest",
                "quarkus.s3.devservices.buckets", "another"), "s3"));
        assertNotEquals(first, serviceConfig(Map.of("quarkus.aws.devservices.floci.image-name", "floci/floci:latest-compat",
                "quarkus.s3.devservices.buckets", "example"), "s3"));
        assertNotEquals(first, serviceConfig(Map.of("quarkus.aws.devservices.shared", "false",
                "quarkus.s3.devservices.buckets", "example"), "s3"));
        assertNotEquals(first, serviceConfig(settings, "s3", "backup"));
        assertEquals(serviceConfig(settings, "s3", "backup"), serviceConfig(settings, "backup", "s3"));
    }

    @Test
    void acceptsRepeatedRequestsWithoutDiscardingDistinctProvisioningSettings() {
        var first = serviceConfig(Map.of("quarkus.s3.devservices.buckets", "first"), "s3", "s3");
        assertEquals(1, first.serviceConfigs().get("s3").size());
        var second = serviceConfig(Map.of("quarkus.s3.devservices.buckets", "second"), "s3");
        var requests = List.of(
                new DevServicesAwsStackProviderBuildItem("s3", first.serviceConfigs().get("s3").iterator().next(), null),
                new DevServicesAwsStackProviderBuildItem("s3", second.serviceConfigs().get("s3").iterator().next(), null));
        var combined = AbstractDevServicesAwsStackProcessor.buildServiceConfig(first.globalConfig(), first.stackConfig(),
                requests);
        assertEquals(2, combined.serviceConfigs().get("s3").size());
    }

    @Test
    void includesNestedAdditionalServiceConfiguration() {
        var first = serviceConfig(Map.of("quarkus.s3.devservices.buckets", "example",
                "quarkus.aws.devservices.floci.additional-services.backup.enabled", "true"), "s3");
        assertEquals(first, serviceConfig(Map.of("quarkus.s3.devservices.buckets", "example",
                "quarkus.aws.devservices.floci.additional-services.backup.enabled", "true"), "s3"));
        assertNotEquals(first, serviceConfig(Map.of("quarkus.s3.devservices.buckets", "example",
                "quarkus.aws.devservices.floci.additional-services.backup.enabled", "false"), "s3"));
    }

    @ConfigMapping(prefix = "quarkus.s3")
    interface ServiceConfig {
        /** Service-specific provisioning settings. */
        ProvisioningConfig devservices();
    }

    interface ProvisioningConfig extends DevServicesBuildTimeConfig {
        /** Buckets to create on the stack. */
        List<String> buckets();
    }
}
