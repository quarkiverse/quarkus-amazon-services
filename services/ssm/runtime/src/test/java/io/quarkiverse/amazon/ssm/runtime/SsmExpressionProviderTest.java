package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sun.net.httpserver.HttpServer;

import io.smallrye.config.PropertiesConfigSourceLoader;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmServiceClientConfiguration;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class SsmExpressionProviderTest {
    @Test
    void singleReadDecryptsAndPassesTheArnAndVersionUnchangedWithBoundedTimeouts() {
        var client = mock(SsmClient.class);
        when(client.serviceClientConfiguration()).thenReturn(SsmServiceClientConfiguration.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder().build()).build());
        when(client.getParameter(any(GetParameterRequest.class))).thenReturn(
                GetParameterResponse.builder().parameter(Parameter.builder().value(" raw\n=value ").build()).build());
        String resource = "arn:aws:ssm:us-east-1:123456789012:parameter/database:3";
        try (var provider = new SsmExpressionProvider(client)) {
            assertEquals(" raw\n=value ", provider.fetch(resource));
        }
        var request = ArgumentCaptor.forClass(GetParameterRequest.class);
        verify(client).getParameter(request.capture());
        assertEquals(resource, request.getValue().name());
        assertTrue(request.getValue().withDecryption());
        assertEquals(Duration.ofSeconds(10),
                request.getValue().overrideConfiguration().orElseThrow().apiCallTimeout().orElseThrow());
        assertEquals(Duration.ofSeconds(3),
                request.getValue().overrideConfiguration().orElseThrow().apiCallAttemptTimeout().orElseThrow());
        verify(client).close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "0S", "5M" })
    void realHandlerUsesProfileFileBootstrapSettingsWithGroupedSourceDisabled(String ttl, @TempDir Path directory)
            throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            assertEquals("AmazonSSM.GetParameter", exchange.getRequestHeaders().getFirst("X-Amz-Target"));
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(request.contains("\"WithDecryption\":true"));
            assertTrue(request.contains("\"Name\":\"/db:3\""));
            byte[] body = "{\"Parameter\":{\"Name\":\"/db\",\"Value\":\"secret-value\",\"Type\":\"SecureString\",\"Version\":3}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.1");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        var base = directory.resolve("application.properties");
        Files.writeString(base, "password=${ssm::/db:3}\n");
        Files.writeString(directory.resolve("application-prod.properties"),
                "quarkus.ssm.endpoint-override=http://127.0.0.1:" + server.getAddress().getPort() + "\n"
                        + "quarkus.ssm.aws.region=us-east-1\nquarkus.ssm.aws.credentials.type=anonymous\n"
                        + "quarkus.ssm.sync-client.type=url\nquarkus.ssm.expression.cache-ttl=" + ttl + "\n");
        var builder = new SmallRyeConfigBuilder().withProfile("prod")
                .withSources(PropertiesConfigSourceLoader.inFileSystem(base.toUri().toString(), 250,
                        Thread.currentThread().getContextClassLoader()));
        var config = new SsmConfigSourceBuilder().configBuilder(builder).build();
        try {
            assertEquals(0, calls.get());
            assertEquals("secret-value", config.getValue("password", String.class));
            assertEquals("secret-value", config.getValue("password", String.class));
            assertEquals(ttl.equals("0S") ? 2 : 1, calls.get());
        } finally {
            for (var source : config.getConfigSources(io.quarkiverse.amazon.common.runtime.AwsConfigExpression.class)) {
                ((io.quarkiverse.amazon.common.runtime.AwsConfigExpression) source).close();
            }
            server.stop(0);
        }
    }
}
