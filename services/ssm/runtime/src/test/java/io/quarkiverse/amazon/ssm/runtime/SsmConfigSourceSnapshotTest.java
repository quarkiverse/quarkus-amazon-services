package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class SsmConfigSourceSnapshotTest {
    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    void failedPageKeepsSnapshotUntilACompleteReload(int failedPage) {
        SsmClient client = mock(SsmClient.class);
        var calls = new AtomicInteger();
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenAnswer(inv -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                return page("old", null);
            }
            if (call == failedPage + 1) {
                throw SdkClientException.create("unsafe-secret");
            }
            return page("new", call == 2 ? "page2" : null);
        });
        try (var source = new SsmConfigSource(client, "/app", List.of(), true, true, Duration.ofHours(1), 270)) {
            source.reload();
            assertEquals("old", source.getValue("app.value"));
            source.reload();
            assertEquals("new", source.getValue("app.value"));
        }
    }

    @Test
    void missingNamedParameterDoesNotPublishPartialResults() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(
                GetParametersResponse.builder().parameters(parameter("/app/a", "old"), parameter("/app/b", "old")).build(),
                GetParametersResponse.builder().parameters(parameter("/app/a", "new")).invalidParameters("/app/b").build(),
                GetParametersResponse.builder().parameters(parameter("/app/a", "new"), parameter("/app/b", "new")).build());
        try (var source = new SsmConfigSource(client, null, List.of("/app/a", "/app/b"), true, true, Duration.ofHours(1),
                270)) {
            source.reload();
            assertEquals(Set.of("app.a", "app.b"), source.getPropertyNames());
            assertEquals("old", source.getValue("app.a"));
            assertEquals("old", source.getValue("app.b"));
            source.reload();
            assertEquals("new", source.getValue("app.a"));
            assertEquals("new", source.getValue("app.b"));
        }
    }

    @Test
    void invalidDeclaredContentKeepsSnapshot() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(
                namedValue("{\"a\":1}"), namedValue("{invalid json"), namedValue("{\"b\":2}"));
        try (var source = new SsmConfigSource(client, null, List.of("/app/doc"), true, true, Duration.ofHours(1), 270,
                ConfigSourceFormat.JSON)) {
            source.reload();
            assertEquals("1", source.getValue("app.doc.a"));
            source.reload();
            assertNull(source.getValue("app.doc.a"));
            assertEquals("2", source.getValue("app.doc.b"));
        }
    }

    private GetParametersByPathResponse page(String value, String nextToken) {
        return GetParametersByPathResponse.builder().parameters(parameter("/app/value", value)).nextToken(nextToken).build();
    }

    private GetParametersResponse namedValue(String value) {
        return GetParametersResponse.builder().parameters(parameter("/app/doc", value)).build();
    }

    private Parameter parameter(String name, String value) {
        return Parameter.builder().name(name).value(value).build();
    }
}
