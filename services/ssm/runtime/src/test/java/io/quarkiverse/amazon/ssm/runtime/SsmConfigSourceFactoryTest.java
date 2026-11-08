package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class SsmConfigSourceFactoryTest {
    @ParameterizedTest
    @ValueSource(strings = { "1H", "PT1H", "3600" })
    void bootstrapMappingAcceptsQuarkusPollingDurations(String value) {
        var config = new SmallRyeConfigBuilder().withMapping(SsmConfigConfig.class)
                .withDefaultValue("quarkus.ssm.config.update-interval", value).build();
        assertEquals(Duration.ofHours(1), config.getConfigMapping(SsmConfigConfig.class).updateInterval());
    }

    @Test
    void legacyPathPrefixShortcutAndNamedEntryShareOneReadWithIndependentFormats() {
        var mapping = mapping(Map.of(
                "quarkus.ssm.config.path", "/app/root/",
                "quarkus.ssm.config.path.\"app.root\"", "/app/root",
                "quarkus.ssm.config.entries.parsed.path", "app/root",
                "quarkus.ssm.config.entries.parsed.prefix", "copy",
                "quarkus.ssm.config.entries.parsed.format", "properties"));
        var client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(
                GetParametersByPathResponse.builder().parameters(
                        Parameter.builder().name("/app/root/db").value("host=localhost\nport=5432").build()).build());
        var source = factory(client).getConfigSources(null, mapping).iterator().next();
        assertEquals("host=localhost\nport=5432", source.getValue("db"));
        assertEquals("host=localhost\nport=5432", source.getValue("app.root.db"));
        assertEquals("localhost", source.getValue("copy.db.host"));
        assertEquals("5432", source.getValue("copy.db.port"));
        verify(client, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "relative", "full" })
    void configuredNamingAndArraySegmentsAreAppliedButNamesRemainFull(String naming) {
        var mapping = mapping(Map.of("quarkus.ssm.config.path", "/app",
                "quarkus.ssm.config.key-naming", naming,
                "quarkus.ssm.config.names", "arn:aws:ssm:us-east-1:123456789012:parameter/outside/key:3"));
        var client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(
                GetParametersByPathResponse.builder().parameters(
                        Parameter.builder().name("/app/servers/_0_/name").value("first").build(),
                        Parameter.builder().name("/app/_0_backup").value("not-an-index").build()).build());
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(GetParametersResponse.builder().parameters(
                Parameter.builder().name("/outside/key").arn("arn:aws:ssm:us-east-1:123456789012:parameter/outside/key")
                        .selector(":3").value("versioned").build())
                .build());
        var source = factory(client).getConfigSources(null, mapping).iterator().next();
        assertEquals("first", source.getValue((naming.equals("full") ? "app.servers._0_.name" : "servers[0].name")));
        assertEquals("not-an-index", source.getValue((naming.equals("full") ? "app." : "") + "_0_backup"));
        assertEquals("versioned", source.getValue("outside.key"));
    }

    @Test
    void aNonrecursiveEntryDoesNotConsumeDescendantsFetchedByAnotherEntry() {
        var mapping = mapping(Map.of("quarkus.ssm.config.entries.shallow.path", "/app",
                "quarkus.ssm.config.entries.shallow.prefix", "shallow",
                "quarkus.ssm.config.entries.shallow.recursive", "false",
                "quarkus.ssm.config.entries.deep.path", "/app",
                "quarkus.ssm.config.entries.deep.prefix", "deep"));
        var client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenAnswer(invocation -> {
            GetParametersByPathRequest request = invocation.getArgument(0);
            var response = GetParametersByPathResponse.builder();
            return request.recursive()
                    ? response.parameters(Parameter.builder().name("/app/nested/value").value("child").build()).build()
                    : response.parameters(Parameter.builder().name("/app/value").value("direct").build()).build();
        });
        var source = factory(client).getConfigSources(null, mapping).iterator().next();
        assertEquals("direct", source.getValue("shallow.value"));
        assertEquals("child", source.getValue("deep.nested.value"));
        assertEquals(null, source.getValue("shallow.nested.value"));
    }

    @Test
    void collidingNamedEntriesFailRatherThanOverwriteEvenIdenticalValues() {
        var mapping = mapping(Map.of("quarkus.ssm.config.entries.a.path", "/app",
                "quarkus.ssm.config.entries.b.path", "/app"));
        var client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(
                GetParametersByPathResponse.builder().parameters(Parameter.builder().name("/app/value").value("same").build())
                        .build());
        var error = assertThrows(IllegalStateException.class, () -> factory(client).getConfigSources(null, mapping));
        assertEquals("Unable to load quarkus.ssm.config [INVALID_CONTENT]", error.getMessage());
        verify(client).close();
    }

    @Test
    void fullPathAndExplicitNameCanSelectTheSameLiteralIndexParameter() {
        var mapping = mapping(Map.of("quarkus.ssm.config.path", "/app",
                "quarkus.ssm.config.key-naming", "full", "quarkus.ssm.config.names", "/app/_1_/token"));
        var parameter = Parameter.builder().name("/app/_1_/token").value("value").build();
        var client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(
                GetParametersByPathResponse.builder().parameters(parameter).build());
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(
                GetParametersResponse.builder().parameters(parameter).build());
        var source = factory(client).getConfigSources(null, mapping).iterator().next();
        assertEquals("value", source.getValue("app._1_.token"));
        assertEquals(null, source.getValue("app[1].token"));
        assertEquals(1, source.getPropertyNames().size());
    }

    @Test
    void explicitIndexNamesStayLiteralWithRelativePathNaming() {
        var mapping = mapping(Map.of("quarkus.ssm.config.names", "/app/_1_/token"));
        var client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(
                GetParametersResponse.builder().parameters(
                        Parameter.builder().name("/app/_1_/token").value("value").build()).build());
        var source = factory(client).getConfigSources(null, mapping).iterator().next();
        assertEquals("value", source.getValue("app._1_.token"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "PT0.5S", "PT-1S" })
    void invalidIntervalsFailBeforeCreatingAClient(String interval) {
        var config = mapping(Map.of("quarkus.ssm.config.names", "/app/value",
                "quarkus.ssm.config.update-interval", interval));
        assertThrows(IllegalArgumentException.class, () -> new SsmConfigSourceFactory().getConfigSources(null, config));
    }

    private SsmConfigConfig mapping(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().withMapping(SsmConfigConfig.class)
                .withDefaultValue("quarkus.ssm.config.enabled", "true")
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(SsmConfigConfig.class);
    }

    private SsmConfigSourceFactory factory(SsmClient client) {
        return new SsmConfigSourceFactory() {
            SsmConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            SsmClient build(SsmConfig config) {
                return client;
            }
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void factoryAppliesStartupPolicyAndOrdinal(boolean required) {
        var mapping = new SmallRyeConfigBuilder().withMapping(SsmConfigConfig.class)
                .withDefaultValue("quarkus.ssm.config.enabled", "true")
                .withDefaultValue("quarkus.ssm.config.names", "/missing")
                .withDefaultValue("quarkus.ssm.config.ordinal", "301")
                .withDefaultValue("quarkus.ssm.config.fail-on-missing-config", Boolean.toString(required))
                .build().getConfigMapping(SsmConfigConfig.class);
        var client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(
                GetParametersResponse.builder().invalidParameters("/missing").build());
        var factory = new SsmConfigSourceFactory() {
            SsmConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            SsmClient build(SsmConfig config) {
                return client;
            }
        };
        if (required) {
            var failure = assertThrows(IllegalStateException.class, () -> factory.getConfigSources(null, mapping));
            assertEquals("Unable to load quarkus.ssm.config [MISSING]", failure.getMessage());
        } else {
            var source = factory.getConfigSources(null, mapping).iterator().next();
            assertEquals(301, source.getOrdinal());
            assertTrue(source.getPropertyNames().isEmpty());
        }
        verify(client).close();
    }
}
