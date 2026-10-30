package io.quarkiverse.amazon.fake.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsCredentialsProviderType;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig.AwsStack;
import io.quarkiverse.amazon.fake.runtime.FakeBuildTimeConfig;
import io.quarkiverse.amazon.fake.runtime.FakeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/** Tests historical provider selection alongside the generic DevServicesSelectionTest, without starting containers. */
class DevServicesSelectionTest extends AbstractDevServicesAwsStackProcessor {
    private DevServicesAwsStackProviderBuildItem provider(Map<String, String> properties) {
        var config = new SmallRyeConfigBuilder().withMapping(FakeBuildTimeConfig.class).withMapping(FakeConfig.class)
                .withMapping(GlobalDevServicesBuildTimeConfig.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build();
        return setup("fake-service", config.getConfigMapping(FakeBuildTimeConfig.class).devservices(),
                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class));
    }

    @Test
    void serviceStackOverridesGlobalProvider() {
        var request = provider(Map.of("quarkus.aws.devservices.provider", "moto",
                "quarkus.fake-service.devservices.provider", "floci"));
        assertNotNull(request);
        assertEquals(AwsStack.FLOCI, request.getConfig().provider().orElseThrow());
    }

    @Test
    void explicitRegionAndIdentityOverrideStackDefaults() {
        var defaults = provider(Map.of()).getDevProvider().getClientConfig().apply(StubAwsStackProcessor.STUB_CONTAINER);
        var config = new SmallRyeConfigBuilder().withMapping(FakeConfig.class)
                .withSources(new MapBackedConfigSource("DevServices", defaults, 240) {
                }, new MapBackedConfigSource("application", Map.of(
                        "quarkus.fake-service.aws.region", "us-west-2",
                        "quarkus.fake-service.aws.credentials.type", "profile",
                        "quarkus.fake-service.aws.credentials.profile-provider.profile-name", "user-profile"), 250) {
                }).build().getConfigMapping(FakeConfig.class).clients().get(ClientUtil.DEFAULT_CLIENT_NAME);
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getEndpoint(), config.sdk().endpointOverride().orElseThrow());
        assertEquals("us-west-2", config.aws().region().orElseThrow().id());
        var credentials = config.aws().credentials().orElseThrow();
        assertEquals(AwsCredentialsProviderType.PROFILE, credentials.type());
        assertEquals("user-profile", credentials.profileProvider().profileName().orElseThrow());
    }
}
