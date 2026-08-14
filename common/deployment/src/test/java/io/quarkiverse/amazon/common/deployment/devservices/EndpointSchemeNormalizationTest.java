package io.quarkiverse.amazon.common.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that {@link AbstractDevServicesAwsStackProcessor#ensureScheme(String)}
 * normalizes endpoint strings so that {@code URI.create()} produces valid URIs
 * with the correct scheme, host, and port.
 */
class EndpointSchemeNormalizationTest {

    static Stream<Arguments> endpointProvider() {
        return Stream.of(
                Arguments.of("http://localhost:4566", "http", "localhost", 4566),
                Arguments.of("https://localhost:4566", "https", "localhost", 4566),
                Arguments.of("localhost:40456", "http", "localhost", 40456));
    }

    @ParameterizedTest
    @MethodSource("endpointProvider")
    void ensureSchemeProducesValidUri(String input, String expectedScheme, String expectedHost, int expectedPort) {
        var result = AbstractDevServicesAwsStackProcessor.ensureScheme(input);
        var uri = URI.create(result);
        assertEquals(expectedScheme, uri.getScheme());
        assertEquals(expectedHost, uri.getHost());
        assertEquals(expectedPort, uri.getPort());
    }

    @Test
    void nullReturnsNull() {
        assertNull(AbstractDevServicesAwsStackProcessor.ensureScheme(null));
    }
}
