package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class SecretsManagerConfigSourcePollingTest {
    @ParameterizedTest
    @ValueSource(strings = { "1H", "PT1H", "3600" })
    void acceptsQuarkusDurations(String interval) {
        assertEquals(Duration.ofHours(1), mapping(interval).updateInterval());
    }

    @ParameterizedTest
    @ValueSource(strings = { "-1S", "100ms" })
    void rejectsInvalidIntervalsBeforeBuildingAClient(String interval) {
        assertThrows(IllegalArgumentException.class,
                () -> new SecretsManagerConfigSourceFactory().getConfigSources(null, mapping(interval)));
    }

    @Test
    void scheduledReadsRetainValuesOnFailureRecoverAndCloseTheClient() throws Exception {
        var client = mock(SecretsManagerClient.class);
        var value = new AtomicReference<String>("initial");
        var failedRead = new CountDownLatch(1);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(invocation -> {
            if (value.get() == null) {
                failedRead.countDown();
                throw new IllegalStateException("unsafe SDK details");
            }
            return GetSecretValueResponse.builder().secretString(value.get()).build();
        });
        var factory = new SecretsManagerConfigSourceFactory() {
            @Override
            SecretsManagerConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            @Override
            SecretsManagerClient build(SecretsManagerConfig config, ConfigSourceContext context) {
                return client;
            }
        };
        try (var source = (SecretsManagerConfigSource) factory.getConfigSources(null, mapping("1S")).iterator().next()) {
            assertEquals("initial", source.getValue("value"));
            verify(client, never()).close();
            value.set(null);
            assertTrue(failedRead.await(5, TimeUnit.SECONDS));
            assertEquals("initial", source.getValue("value"));
            value.set("updated");
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (!"updated".equals(source.getValue("value"))) {
                    Thread.sleep(10);
                }
            });
        }
        verify(client).close();
    }

    private static SecretsManagerConfigConfig mapping(String interval) {
        return new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                .withDefaultValue("quarkus.secretsmanager.config.enabled", "true")
                .withDefaultValue("quarkus.secretsmanager.config.secrets.value", "secret")
                .withDefaultValue("quarkus.secretsmanager.config.format", "raw")
                .withDefaultValue("quarkus.secretsmanager.config.update-interval", interval)
                .build().getConfigMapping(SecretsManagerConfigConfig.class);
    }
}
