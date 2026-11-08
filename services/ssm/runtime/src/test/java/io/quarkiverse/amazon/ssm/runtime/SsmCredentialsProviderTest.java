package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.SsmException;

class SsmCredentialsProviderTest {
    @ParameterizedTest
    @ValueSource(strings = { "json", "properties", "raw" })
    void documentFormatsAllowExternalUsername(String format) {
        var provider = provider(Map.of("parameter-name", "/database", "format", format));
        String text = switch (format) {
            case "json" -> "{\"password\":\"secret\"}";
            case "properties" -> "password=secret";
            default -> "secret";
        };
        when(provider.client.getParameter(any(GetParameterRequest.class))).thenReturn(response("/database", text));
        var credentials = provider.getCredentials("db");
        assertEquals(Map.of("password", "secret"), credentials);
        assertThrows(UnsupportedOperationException.class, () -> credentials.put("password", "other"));
        var request = ArgumentCaptor.forClass(GetParameterRequest.class);
        verify(provider.client).getParameter(request.capture());
        assertEquals("/database", request.getValue().name());
        assertEquals(true, request.getValue().withDecryption());
        verify(provider.client, never()).close();
    }

    @Test
    void configurableKeysAndNamedSetsUseTheirOwnDocuments() {
        var provider = provider(Map.of("parameter-name", "/database", "username-key", "login", "password-key", "pwd"));
        when(provider.client.getParameter(any(GetParameterRequest.class)))
                .thenReturn(response("/database", "{\"login\":\"app\",\"pwd\":\"secret\",\"expires-at\":\"ignored\"}"));
        assertEquals(Map.of("user", "app", "password", "secret"), provider.getCredentials("db"));
        var local = provider(Map.of("parameter-name", "/database", "username-key", ""));
        when(local.client.getParameter(any(GetParameterRequest.class)))
                .thenReturn(response("/database", "{\"username\":\"ignored\",\"password\":\"secret\"}"));
        assertEquals(Map.of("password", "secret"), local.getCredentials("db"));

        provider.config = new SmallRyeConfigBuilder().withMapping(SsmCredentialsProviderConfig.class)
                .withDefaultValue("quarkus.ssm.credentials-provider.other.parameter-name", "/other")
                .build().getConfigMapping(SsmCredentialsProviderConfig.class);
        when(provider.client.getParameter(any(GetParameterRequest.class)))
                .thenReturn(response("/other", "{\"password\":\"other\"}"));
        assertEquals(Map.of("password", "other"), provider.getCredentials("other"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rawKeysDeduplicateAndBatchResponsesCanBeUnordered(boolean batch) {
        var provider = provider(Map.of("parameters.user", "/login", "parameters.password", "/pwd",
                "batch-retrieval", Boolean.toString(batch)));
        when(provider.client.getParameter(any(GetParameterRequest.class))).thenAnswer(call -> {
            GetParameterRequest request = call.getArgument(0);
            return response(request.name(), request.name().equals("/login") ? "app" : "secret");
        });
        when(provider.client.getParameters(any(GetParametersRequest.class))).thenReturn(GetParametersResponse.builder()
                .parameters(value("/pwd", "secret"), value("/login", "app")).build());
        assertEquals(Map.of("user", "app", "password", "secret"), provider.getCredentials("db"));
        verify(provider.client, times(batch ? 0 : 2)).getParameter(any(GetParameterRequest.class));
        verify(provider.client, times(batch ? 1 : 0)).getParameters(any(GetParametersRequest.class));
        if (batch) {
            var request = ArgumentCaptor.forClass(GetParametersRequest.class);
            verify(provider.client).getParameters(request.capture());
            assertEquals(true, request.getValue().withDecryption());
        }
        var shared = provider(Map.of("parameters.user", "/shared", "parameters.password", "/shared",
                "batch-retrieval", Boolean.toString(batch)));
        when(shared.client.getParameter(any(GetParameterRequest.class))).thenReturn(response("/shared", "same"));
        when(shared.client.getParameters(any(GetParametersRequest.class)))
                .thenReturn(GetParametersResponse.builder().parameters(value("/shared", "same")).build());
        assertEquals(Map.of("user", "same", "password", "same"), shared.getCredentials("db"));
        verify(shared.client, times(batch ? 0 : 1)).getParameter(any(GetParameterRequest.class));
        if (batch) {
            var request = ArgumentCaptor.forClass(GetParametersRequest.class);
            verify(shared.client).getParameters(request.capture());
            assertEquals(java.util.List.of("/shared"), request.getValue().names());
        }
    }

    @Test
    void batchedNamesArnsAndVersionSelectorsCorrelateToConfiguredIdentities() {
        String arn = "arn:aws:ssm:us-east-1:123456789012:parameter/database";
        var provider = provider(Map.of("parameters.user", "/database:current", "parameters.password", arn + ":current",
                "batch-retrieval", "true"));
        when(provider.client.getParameters(any(GetParametersRequest.class))).thenReturn(GetParametersResponse.builder()
                .parameters(value("/database", "same").toBuilder().arn(arn).selector(":current").build()).build());
        assertEquals(Map.of("user", "same", "password", "same"), provider.getCredentials("db"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "omitted", "duplicate", "unknown", "contradiction", "missing", "unknown-error" })
    void incompleteOrMalformedBatchesNeverReturnPartialCredentials(String scenario) {
        var provider = provider(Map.of("parameters.user", "/login", "parameters.password", "/pwd", "batch-retrieval", "true"));
        var response = GetParametersResponse.builder();
        switch (scenario) {
            case "duplicate" -> response.parameters(value("/login", "app"), value("/pwd", "secret"), value("/pwd", "other"));
            case "unknown" -> response.parameters(value("/login", "app"), value("/pwd", "secret"), value("/other", "unsafe"));
            case "contradiction" -> response.parameters(value("/login", "app"), value("/pwd", "secret"))
                    .invalidParameters("/pwd");
            case "missing" -> response.parameters(value("/login", "app")).invalidParameters("/pwd");
            case "unknown-error" -> response.parameters(value("/login", "app"), value("/pwd", "secret"))
                    .invalidParameters("/unknown");
            default -> response.parameters(value("/login", "app"));
        }
        when(provider.client.getParameters(any(GetParametersRequest.class))).thenReturn(response.build());
        assertSafeFailure(provider, scenario.equals("missing") ? "MISSING" : "PARTIAL_RESPONSE");
        verify(provider.client, never()).getParameter(any(GetParameterRequest.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void failuresDoNotCacheOrFallbackAndFreshReadsCanRecover(boolean batch) {
        var provider = provider(Map.of("parameter-name", "/pwd", "format", "raw", "batch-retrieval", Boolean.toString(batch)));
        var failure = SsmException.builder().statusCode(403).message("unsafe parameter credentials").build();
        when(provider.client.getParameter(any(GetParameterRequest.class)))
                .thenReturn(response("/pwd", "old")).thenThrow(failure).thenReturn(response("/pwd", "new"));
        when(provider.client.getParameters(any(GetParametersRequest.class)))
                .thenReturn(GetParametersResponse.builder().parameters(value("/pwd", "old")).build())
                .thenThrow(failure).thenReturn(GetParametersResponse.builder().parameters(value("/pwd", "new")).build());
        assertEquals(Map.of("password", "old"), provider.getCredentials("db"));
        assertSafeFailure(provider, "ACCESS_DENIED");
        assertEquals(Map.of("password", "new"), provider.getCredentials("db"));
        if (batch) {
            verify(provider.client, never()).getParameter(any(GetParameterRequest.class));
        }
    }

    @Test
    void invalidSelectionsAndUnknownSetsNeverReadAws() {
        for (var properties : java.util.List.of(
                Map.of("parameter-name", "/db", "parameters.password", "/other"),
                Map.of("parameters.user", "/login"),
                Map.of("parameters.password", "/pwd", "parameters.expires-at", "/expiry"),
                Map.of("parameter-name", "/db", "format", "auto"),
                Map.of("parameter-name", " "))) {
            var provider = provider(properties);
            assertSafeFailure(provider, "INVALID_CONTENT");
            for (String name : new String[] { null, "", "unknown" }) {
                var failure = assertThrows(IllegalArgumentException.class, () -> provider.getCredentials(name));
                assertEquals("Unknown SSM credentials provider.", failure.getMessage());
                assertNull(failure.getCause());
            }
            verifyNoInteractions(provider.client);
        }
    }

    @Test
    void concurrentDocumentReadsDoNotMixVersions() throws Exception {
        var provider = provider(Map.of("parameter-name", "/database"));
        var counter = new AtomicInteger();
        when(provider.client.getParameter(any(GetParameterRequest.class))).thenAnswer(call -> {
            int version = counter.incrementAndGet();
            return response("/database", "{\"username\":\"u" + version + "\",\"password\":\"p" + version + "\"}");
        });
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var calls = new java.util.ArrayList<java.util.concurrent.Future<Map<String, String>>>();
            for (int i = 0; i < 16; i++) {
                calls.add(executor.submit(() -> provider.getCredentials("db")));
            }
            for (var call : calls) {
                var credentials = call.get(5, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals("p" + credentials.get("user").substring(1), credentials.get("password"));
            }
        }
        assertEquals(16, counter.get());
    }

    private static void assertSafeFailure(SsmCredentialsProvider provider, String category) {
        var failure = assertThrows(IllegalStateException.class, () -> provider.getCredentials("db"));
        assertEquals("Unable to load SSM credentials [" + category + "]", failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    private static SsmCredentialsProvider provider(Map<String, String> properties) {
        var values = new HashMap<String, String>();
        properties.forEach((key, value) -> values.put("quarkus.ssm.credentials-provider.db." + key, value));
        var provider = new SsmCredentialsProvider();
        provider.config = new SmallRyeConfigBuilder().withMapping(SsmCredentialsProviderConfig.class)
                .withSources(new MapBackedConfigSource("test", values) {
                }).build().getConfigMapping(SsmCredentialsProviderConfig.class);
        provider.client = mock(SsmClient.class);
        return provider;
    }

    private static Parameter value(String name, String content) {
        return Parameter.builder().name(name).value(content).build();
    }

    private static GetParameterResponse response(String name, String content) {
        return GetParameterResponse.builder().parameter(value(name, content)).build();
    }
}
