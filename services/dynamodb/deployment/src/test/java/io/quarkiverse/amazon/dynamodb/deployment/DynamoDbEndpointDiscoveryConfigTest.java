package io.quarkiverse.amazon.dynamodb.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.dynamodb.runtime.DynamoDbConfig;
import io.quarkiverse.amazon.dynamodb.runtime.DynamoDbRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class DynamoDbEndpointDiscoveryConfigTest {
    @Test
    void namedClientsInheritAndCanExplicitlyDisableDiscovery() throws Exception {
        DynamoDbConfig config = config(Map.of(
                "quarkus.dynamodb.enable-endpoint-discovery", "true",
                "quarkus.dynamodb.\"disabled\".enable-endpoint-discovery", "false",
                "quarkus.dynamodb.\"inherited\".aws.region", "eu-west-1"));
        assertDiscovery(config, ClientUtil.DEFAULT_CLIENT_NAME, true);
        assertDiscovery(config, "disabled", false);
        assertDiscovery(config, "inherited", true);
        assertDiscovery(config, "unconfigured", true);
        assertEquals("eu-west-1", config.clients().get("inherited").aws().region().orElseThrow().id());
    }

    @Test
    void namedClientsCanEnableDiscoveryWhenDisabledByDefault() throws Exception {
        DynamoDbConfig config = config(Map.of(
                "quarkus.dynamodb.enable-endpoint-discovery", "false",
                "quarkus.dynamodb.\"enabled\".enable-endpoint-discovery", "true"));
        assertDiscovery(config, ClientUtil.DEFAULT_CLIENT_NAME, false);
        assertDiscovery(config, "enabled", true);
        assertDiscovery(config, "unconfigured", false);
    }

    @Test
    void absentSettingsLeaveTheSdkDefaultUntouched() throws Exception {
        DynamoDbConfig config = config(Map.of(
                "quarkus.dynamodb.\"enabled\".enable-endpoint-discovery", "true"));
        assertDiscovery(config, ClientUtil.DEFAULT_CLIENT_NAME, null);
        assertDiscovery(config, "unconfigured", null);
        assertDiscovery(config, "enabled", true);
    }

    private void assertDiscovery(DynamoDbConfig config, String clientName, Boolean expected) throws Exception {
        DynamoDbRecorder recorder = new DynamoDbRecorder(new RuntimeValue<>(config));
        assertEquals(expected, config.enableEndpointDiscovery(clientName).orElse(null));
        for (Object builder : new Object[] { recorder.getSyncBuilder(clientName).getValue(),
                recorder.getAsyncBuilder(clientName).getValue() }) {
            // The SDK exposes no getter for endpoint discovery on its client builders.
            var field = builder.getClass().getSuperclass().getDeclaredField("endpointDiscoveryEnabled");
            field.setAccessible(true);
            assertEquals(Boolean.TRUE.equals(expected), field.getBoolean(builder), clientName);
        }
    }

    private DynamoDbConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(DynamoDbConfig.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(DynamoDbConfig.class);
    }
}
