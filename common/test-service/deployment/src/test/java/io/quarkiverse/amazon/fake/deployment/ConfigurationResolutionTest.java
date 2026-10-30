package io.quarkiverse.amazon.fake.deployment;

import java.util.Map;

import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.test.AbstractConfigurationResolutionTest;
import io.quarkiverse.amazon.fake.runtime.FakeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/** Runs the same inheritance and diagnostic edge cases as the generic SDK ConfigurationResolutionTest. */
class ConfigurationResolutionTest extends AbstractConfigurationResolutionTest {
    @Override
    protected HasAmazonClientRuntimeConfig configuration(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(FakeConfig.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(FakeConfig.class);
    }

    @Override
    protected String prefix() {
        return "quarkus.fake-service";
    }

    @Override
    protected String awsSuffix() {
        return ".aws";
    }
}
