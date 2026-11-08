package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerServiceClientConfiguration;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class SecretsManagerExpressionProviderTest {
    @Test
    void singleReadPreservesArnAndRawTextAndHonorsServiceTimeouts() throws Exception {
        var client = client(ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(7))
                .apiCallAttemptTimeout(Duration.ofSeconds(2)).build());
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretString(" raw\n=value ").build());
        String resource = "arn:aws:secretsmanager:us-east-1:123456789012:secret:database-AbCdEf";
        try (var provider = new SecretsManagerExpressionProvider(client)) {
            assertEquals(" raw\n=value ", provider.fetch(resource));
        }
        var request = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(client).getSecretValue(request.capture());
        assertEquals(resource, request.getValue().secretId());
        assertEquals(Duration.ofSeconds(7),
                request.getValue().overrideConfiguration().orElseThrow().apiCallTimeout().orElseThrow());
        assertEquals(Duration.ofSeconds(2),
                request.getValue().overrideConfiguration().orElseThrow().apiCallAttemptTimeout().orElseThrow());
        verify(client).close();
    }

    @Test
    void binaryUtf8IsStrictAndFailuresHaveNoValueOrCause() {
        var client = client(ClientOverrideConfiguration.builder().build());
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray("été".getBytes(StandardCharsets.UTF_8)))
                        .build(),
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(new byte[] { (byte) 0xff, (byte) 0xfe }))
                        .build());
        try (var handler = new AwsConfigExpression("sm", "secretsmanager",
                context -> new SecretsManagerExpressionProvider(client))) {
            var config = new SmallRyeConfigBuilder().addDefaultInterceptors().withSecretKeyHandlerFactories(handler)
                    .withSources(handler).withSources(new MapBackedConfigSource("test", Map.of(
                            "value", "${sm::binary}", "quarkus.secretsmanager.expression.cache-ttl", "PT0S")) {
                    }).build();
            assertEquals("été", config.getValue("value", String.class));
            var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertEquals("Unable to resolve AWS sm expression; check resource, permissions, content and client settings.",
                    failure.getMessage());
            assertNull(failure.getCause());
        }
        verify(client).close();
    }

    private static SecretsManagerClient client(ClientOverrideConfiguration overrides) {
        var client = mock(SecretsManagerClient.class);
        when(client.serviceClientConfiguration()).thenReturn(SecretsManagerServiceClientConfiguration.builder()
                .overrideConfiguration(overrides).build());
        return client;
    }
}
