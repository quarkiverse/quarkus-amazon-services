package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.s3.runtime.S3Config;
import io.quarkiverse.amazon.s3.runtime.S3Recorder;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class S3MultipartConfigTest {
    @Test
    void multipartIsDisabledByDefaultAndSizesAreLeftToTheSdk() {
        S3Config config = config(Map.of());
        assertFalse(config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().enabled().orElse(false));
        assertTrue(config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().thresholdInBytes().isEmpty());
        assertTrue(config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().minimumPartSizeInBytes().isEmpty());
        new S3Recorder(new RuntimeValue<>(config)).getAsyncClientBuilder();
    }

    @Test
    void multipartCanBeEnabledWithoutExplicitSizes() {
        new S3Recorder(new RuntimeValue<>(config(Map.of("quarkus.s3.multipart.enabled", "true"))))
                .getAsyncClientBuilder();
    }

    @Test
    void sizesAcceptUnitsAndByteCounts() {
        S3Config config = config(Map.of(
                "quarkus.s3.multipart.enabled", "true",
                "quarkus.s3.multipart.threshold-in-bytes", "100000000",
                "quarkus.s3.multipart.minimum-part-size-in-bytes", "8M"));
        assertEquals(100000000L,
                config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().thresholdInBytes().orElseThrow().asLongValue());
        assertEquals(8388608L, config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().minimumPartSizeInBytes()
                .orElseThrow().asLongValue());
        new S3Recorder(new RuntimeValue<>(config)).getAsyncClientBuilder();
    }

    @Test
    void sizesOutsideTheSdkLongRangeAreRejected() {
        for (String property : new String[] { "threshold-in-bytes", "minimum-part-size-in-bytes" }) {
            S3Recorder recorder = new S3Recorder(new RuntimeValue<>(config(Map.of(
                    "quarkus.s3.multipart.enabled", "true", "quarkus.s3.multipart." + property, "8E"))));
            assertThrows(ArithmeticException.class, recorder::getAsyncClientBuilder);
        }
    }

    @Test
    void enabledMultipartRejectsNonPositiveSizes() {
        for (String property : new String[] { "threshold-in-bytes", "minimum-part-size-in-bytes" }) {
            String key = "quarkus.s3.multipart." + property;
            for (String value : new String[] { "0", "-1" }) {
                S3Recorder recorder = new S3Recorder(new RuntimeValue<>(config(Map.of(
                        "quarkus.s3.multipart.enabled", "true", key, value))));
                IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                        recorder::getAsyncClientBuilder);
                assertTrue(failure.getMessage().contains(key));
            }
        }
    }

    @Test
    void sizesAloneDoNotEnableMultipart() {
        S3Config config = config(Map.of("quarkus.s3.multipart.threshold-in-bytes", "100000000"));
        assertFalse(config.s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart().enabled().orElse(false));
        new S3Recorder(new RuntimeValue<>(config)).getAsyncClientBuilder();
    }

    private S3Config config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(S3Config.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(S3Config.class);
    }
}
