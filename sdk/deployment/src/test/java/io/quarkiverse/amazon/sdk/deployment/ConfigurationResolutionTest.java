package io.quarkiverse.amazon.sdk.deployment;

import java.util.Map;

import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.test.AbstractConfigurationResolutionTest;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkConfig;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

/** Runs the same inheritance and diagnostic edge cases as the historical ConfigurationResolutionTest. */
class ConfigurationResolutionTest extends AbstractConfigurationResolutionTest {
    @Override
    protected HasAmazonClientRuntimeConfig configuration(Map<String, String> properties) {
        var config = new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(AmazonSdkConfig.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(AmazonSdkConfig.class);
        return new AmazonSdkRecorder(new RuntimeValue<>(config)).configuration().getValue();
    }

    @Override
    protected String prefix() {
        return "quarkus.aws";
    }

    @Override
    protected String awsSuffix() {
        return "";
    }
}
