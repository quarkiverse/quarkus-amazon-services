package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.ResourceId;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class SsmConfigProviderTest {
    private final ResourceId a = new ResourceId("/app/a");
    private final ResourceId b = new ResourceId("/app/b");

    @Test
    void secondPageFailureRetainsReceivedDocumentsWithoutInferringRemoval() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(
                GetParametersByPathResponse.builder().parameters(parameter(a), parameter(b)).build(),
                GetParametersByPathResponse.builder().parameters(parameter(a)).nextToken("next").build())
                .thenThrow(SdkClientException.create("network"))
                .thenReturn(GetParametersByPathResponse.builder().parameters(parameter(a)).build());
        try (var provider = new SsmConfigProvider(client, "/app", List.of(), true, true)) {
            provider.fetchAll();
            var partial = provider.fetchAll();
            assertNotNull(partial.batchFailure());
            assertEquals(Set.of(a), partial.updated().keySet());
            assertTrue(partial.removed().isEmpty());
            assertEquals(Set.of(b), provider.fetchAll().removed());
        }
    }

    @Test
    void firstPageFailureIsABatchFailureEvenWithoutKnownResources() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenThrow(SdkClientException.create("network"));
        try (var provider = new SsmConfigProvider(client, "/app", List.of(), true, true)) {
            var result = provider.fetchAll();
            assertNotNull(result.batchFailure());
            assertTrue(result.failed().isEmpty());
            assertTrue(result.removed().isEmpty());
        }
    }

    @Test
    void unreportedNamesAreAnIncompleteBatch() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class)))
                .thenReturn(GetParametersResponse.builder().parameters(parameter(a)).build());
        try (var provider = new SsmConfigProvider(client, null, List.of(a.value(), b.value()), true, true)) {
            assertEquals(FetchFailure.PARTIAL_RESPONSE, provider.fetchAll().batchFailure());
        }
    }

    @Test
    void arnAndVersionSelectorsKeepCanonicalParameterNaming() {
        SsmClient client = mock(SsmClient.class);
        String arn = "arn:aws:ssm:eu-west-1:123456789012:parameter/app/a";
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(GetParametersResponse.builder()
                .parameters(Parameter.builder().name(a.value()).arn(arn).selector(":3").version(3L).value("value").build())
                .build());
        try (var source = new SsmConfigSource(client, null, List.of(arn + ":3"), true, true, java.time.Duration.ZERO, 270)) {
            assertEquals("value", source.getValue("app.a"));
        }
    }

    private Parameter parameter(ResourceId id) {
        return Parameter.builder().name(id.value()).value("value").version(1L).build();
    }
}
