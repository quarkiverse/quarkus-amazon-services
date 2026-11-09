package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.s3.runtime.S3Config;
import io.quarkiverse.amazon.s3.runtime.S3Recorder;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

class S3NamedMultipartConfigTest {
    @Test
    void namedMultipartSettingsFallBackPerPropertyAndAllowExplicitDisabling() throws Exception {
        S3Config config = new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(S3Config.class)
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "quarkus.s3.path-style-access", "true",
                        "quarkus.s3.multipart.enabled", "true",
                        "quarkus.s3.multipart.threshold-in-bytes", "4K",
                        "quarkus.s3.multipart.minimum-part-size-in-bytes", "1K",
                        "quarkus.s3.\"archive\".multipart.threshold-in-bytes", "2K",
                        "quarkus.s3.\"large-parts\".multipart.minimum-part-size-in-bytes", "2K",
                        "quarkus.s3.\"disabled\".multipart.enabled", "false")) {
                }).build().getConfigMapping(S3Config.class);
        S3Recorder recorder = new S3Recorder(new RuntimeValue<>(config));
        AtomicInteger parts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("partNumber")) {
                parts.incrementAndGet();
            }
            String response = exchange.getRequestMethod().equals("POST")
                    ? query != null && query.contains("uploads")
                            ? "<InitiateMultipartUploadResult><Bucket>bucket</Bucket><Key>key</Key><UploadId>upload</UploadId></InitiateMultipartUploadResult>"
                            : "<CompleteMultipartUploadResult><Bucket>bucket</Bucket><Key>key</Key><ETag>\"etag\"</ETag></CompleteMultipartUploadResult>"
                    : "";
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("ETag", "\"etag\"");
            exchange.sendResponseHeaders(200, body.length == 0 ? -1 : body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            URI endpoint = URI.create("http://localhost:" + server.getAddress().getPort());
            assertParts(recorder, ClientUtil.DEFAULT_CLIENT_NAME, endpoint, parts, 3, 0);
            assertParts(recorder, "archive", endpoint, parts, 3, 3);
            assertParts(recorder, "large-parts", endpoint, parts, 6, 3);
            assertParts(recorder, "disabled", endpoint, parts, 6, 0);
            assertParts(recorder, "unconfigured", endpoint, parts, 6, 6);
        } finally {
            server.stop(0);
        }
    }

    private void assertParts(S3Recorder recorder, String clientName, URI endpoint, AtomicInteger parts, int sizeInKiB,
            int expectedParts) throws Exception {
        parts.set(0);
        S3AsyncClientBuilder builder = (S3AsyncClientBuilder) recorder.getAsyncBuilder(clientName).getValue();
        try (S3AsyncClient client = builder.endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()).build()) {
            client.putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromBytes(new byte[sizeInKiB * 1024]))
                    .get(10, TimeUnit.SECONDS);
            assertEquals(expectedParts, parts.get(), clientName);
        }
    }
}
