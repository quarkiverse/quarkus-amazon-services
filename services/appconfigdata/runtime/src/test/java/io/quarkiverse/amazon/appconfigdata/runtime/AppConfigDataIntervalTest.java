package io.quarkiverse.amazon.appconfigdata.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.smallrye.config.SmallRyeConfigBuilder;

class AppConfigDataIntervalTest {
    @ParameterizedTest
    @CsvSource({ "update-interval,PT0.5S", "update-interval,PT-1S", "update-interval-minutes,-1" })
    void rejectsInvalidIntervalsBeforeClientCreation(String property, String value) {
        var config = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true")
                .withDefaultValue("quarkus.appconfigdata.config.application", "app")
                .withDefaultValue("quarkus.appconfigdata.config.environment", "env")
                .withDefaultValue("quarkus.appconfigdata.config.configuration-profile", "profile")
                .withDefaultValue("quarkus.appconfigdata.config." + property, value)
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfigDataConfigSourceFactory().getConfigSources(null, config));
    }
}
