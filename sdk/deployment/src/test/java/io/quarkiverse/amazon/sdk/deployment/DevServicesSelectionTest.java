package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig.AwsStack;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkBuildTimeConfig;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Verifies per-name provider selection and inheritance, request grouping, endpoint opt-out, and preservation of explicit client
 * settings.
 * Historical counterpart: {@code io.quarkiverse.amazon.fake.deployment.devservices.DevServicesSelectionTest}.
 */
class DevServicesSelectionTest {
    private static SmallRyeConfig config(Map<String, String> values) {
        return new SmallRyeConfigBuilder().withSources(new MapBackedConfigSource("test", values) {
        }).withValidateUnknown(false)
                .withMapping(AmazonSdkBuildTimeConfig.class).withMapping(GlobalDevServicesBuildTimeConfig.class).build();
    }

    @ParameterizedTest
    @ValueSource(strings = { "quarkus.aws", "quarkus.aws.remote", "quarkus.aws.\"remote\"" })
    void anyEffectiveEndpointOptsOut(String prefix) {
        SmallRyeConfig config = config(Map.of(prefix + ".endpoint-override", "https://external.example"));
        var settings = config.getConfigMapping(AmazonSdkBuildTimeConfig.class).devservices();
        assertNull(AmazonSdkDevServicesProcessor.provider("ecr", List.of("remote"), settings, config));
    }

    @Test
    void sharesOneStackConfigurationAcrossServicesAndNames() {
        SmallRyeConfig config = config(Map.of(
                "quarkus.aws.devservices.enabled", "false",
                "quarkus.aws.devservices.provider", "moto",
                "quarkus.aws.devservices.container-properties.COMMON", "global"));
        var root = config.getConfigMapping(AmazonSdkBuildTimeConfig.class);
        var settings = root.devservices();
        assertEquals(false, settings.enabled().orElseThrow());
        assertEquals(AwsStack.MOTO, settings.provider().orElseThrow());
        assertEquals("global", settings.containerProperties().get("COMMON"));
        assertNull(AmazonSdkDevServicesProcessor.provider("ecr", List.of(ClientUtil.DEFAULT_CLIENT_NAME), settings, config));
        assertNull(AmazonSdkDevServicesProcessor.provider("sesv2", List.of("local"), settings, config));
    }

    @Test
    void selectsStacksPerNameAndPublishesOnlyTheirClientDefaults() {
        SmallRyeConfig config = config(Map.of("quarkus.aws.devservices.provider", "floci",
                "quarkus.aws.\"other\".devservices.provider", "moto",
                "quarkus.aws.\"remote\".devservices.provider", "localstack",
                "quarkus.aws.\"remote\".endpoint-override", "https://external.example"));
        var requests = AmazonSdkDevServicesProcessor.providers("sesv2",
                List.of(ClientUtil.DEFAULT_CLIENT_NAME, "local", "other", "remote"),
                config.getConfigMapping(AmazonSdkBuildTimeConfig.class),
                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class), config);
        assertEquals(2, requests.size());
        var byStack = requests.stream().collect(java.util.stream.Collectors.toMap(
                request -> request.getConfig().provider().orElseThrow(),
                request -> request.getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STACK)));
        assertEquals(java.util.Set.of("quarkus.aws.\"<default>\".endpoint-override", "quarkus.aws.local.endpoint-override"),
                byStack.get(AwsStack.FLOCI).keySet().stream().filter(key -> key.endsWith(".endpoint-override"))
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("quarkus.aws.\"other\".endpoint-override"),
                byStack.get(AwsStack.MOTO).keySet().stream().filter(key -> key.endsWith(".endpoint-override"))
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "quarkus.aws.local", "quarkus.aws.\"local\"", "quarkus.aws.\"local.eu\"" })
    void namedStackOverridesGlobalProvider(String prefix) {
        SmallRyeConfig config = config(Map.of("quarkus.aws.devservices.provider", "moto",
                prefix + ".devservices.provider", "floci"));
        var requests = AmazonSdkDevServicesProcessor.providers("sesv2",
                List.of(prefix.contains("local.eu") ? "local.eu" : "local"),
                config.getConfigMapping(AmazonSdkBuildTimeConfig.class),
                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class), config);
        assertEquals(AwsStack.FLOCI, requests.getFirst().getConfig().provider().orElseThrow());
    }

    @Test
    void namedClientsInheritGlobalProvider() {
        SmallRyeConfig config = config(Map.of("quarkus.aws.devservices.provider", "moto"));
        var requests = AmazonSdkDevServicesProcessor.providers("sesv2", List.of("local", "other"),
                config.getConfigMapping(AmazonSdkBuildTimeConfig.class),
                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class), config);
        assertEquals(1, requests.size());
        assertEquals(AwsStack.MOTO, requests.getFirst().getConfig().provider().orElseThrow());
    }

    @Test
    void sameNameUsesTheSameStackDefaultsRegardlessOfService() {
        SmallRyeConfig config = config(Map.of());
        var settings = config.getConfigMapping(AmazonSdkBuildTimeConfig.class).devservices();
        var ecr = AmazonSdkDevServicesProcessor.provider("ecr", List.of("local"), settings, config);
        var ses = AmazonSdkDevServicesProcessor.provider("sesv2", List.of("local"), settings, config);
        assertEquals(ecr.getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STACK),
                ses.getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STACK));
    }

    @ParameterizedTest
    @ValueSource(strings = { "quarkus.aws.local", "quarkus.aws.\"local\"", "quarkus.aws.\"local.eu\"" })
    void publishesDefaultsOnTheExistingClientEntry(String prefix) {
        SmallRyeConfig config = config(Map.of(prefix + ".region", "eu-west-1"));
        var settings = config.getConfigMapping(AmazonSdkBuildTimeConfig.class).devservices();
        String name = prefix.contains("local.eu") ? "local.eu" : "local";
        var provider = AmazonSdkDevServicesProcessor.provider("ecr", List.of(name), settings, config);
        var defaults = provider.getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STACK);
        assertEquals("http://stub-aws:4566", defaults.get(prefix + ".endpoint-override"));
        assertFalse(defaults.containsKey(prefix + ".region"));
    }

    @Test
    void leavesExplicitRegionAndCredentialGroupUntouched() {
        SmallRyeConfig config = config(Map.of("quarkus.aws.region", "eu-west-1",
                "quarkus.aws.credentials.static-provider.access-key-id", "partial-identity"));
        var settings = config.getConfigMapping(AmazonSdkBuildTimeConfig.class).devservices();
        var provider = AmazonSdkDevServicesProcessor.provider("ecr", List.of("local"), settings, config);
        var defaults = provider.getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STACK);
        assertEquals(Map.of("quarkus.aws.local.endpoint-override", "http://stub-aws:4566"), defaults);
        assertFalse(defaults.keySet().stream().anyMatch(key -> key.contains("credentials")));
    }

    @Test
    void doesNotRequestUnusedSdkServices() {
        SmallRyeConfig config = config(Map.of());
        var settings = config.getConfigMapping(AmazonSdkBuildTimeConfig.class).devservices();
        assertNull(AmazonSdkDevServicesProcessor.provider("ecr", List.of(), settings, config));
    }
}
