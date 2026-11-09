package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpServer;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

class S3NamedTransferManagerTest {
    @Inject
    CustomProducer producer;
    @Inject
    @S3Crt
    S3TransferManager defaultCrt;
    static final HttpServer archiveServer = server("archive");
    static final HttpServer otherServer = server("other");
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(CustomProducer.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.s3.path-style-access", "true")
            .overrideConfigKey("quarkus.s3.\"archive\".endpoint-override",
                    "http://localhost:" + archiveServer.getAddress().getPort())
            .overrideConfigKey("quarkus.s3.\"other\".endpoint-override",
                    "http://localhost:" + otherServer.getAddress().getPort());

    @Inject
    @AmazonClient("archive")
    S3TransferManager archive;
    @Inject
    @S3Crt
    @AmazonClient("archive")
    S3TransferManager archiveCrt;
    @Inject
    @AmazonClient("other")
    S3TransferManager other;
    @Inject
    @S3Crt
    @AmazonClient("other")
    S3TransferManager otherCrt;

    @Test
    void managersAloneDiscoverTheirNamedClientsAndUseTheRightEndpoint() throws Exception {
        assertEquals("archive", download(archive));
        assertEquals("archive", download(archiveCrt));
        assertEquals("other", download(other));
        assertEquals("other", download(otherCrt));
        org.junit.jupiter.api.Assertions.assertNotSame(io.quarkus.arc.ClientProxy.unwrap(defaultCrt),
                io.quarkus.arc.ClientProxy.unwrap(archiveCrt));
        assertEquals(1, producer.created());
        archive.close();
        archiveCrt.close();
        var client = io.quarkus.arc.Arc.container().instance(software.amazon.awssdk.services.s3.S3AsyncClient.class,
                new AmazonClient.AmazonClientLiteral("archive")).get();
        var crtClient = io.quarkus.arc.Arc.container().instance(software.amazon.awssdk.services.s3.S3AsyncClient.class,
                new AmazonClient.AmazonClientLiteral("archive"), S3Crt.Literal.INSTANCE).get();
        assertEquals("archive", client.getObject(r -> r.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes())
                .get(10, TimeUnit.SECONDS).asUtf8String());
        assertEquals("archive", crtClient.getObject(r -> r.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes())
                .get(10, TimeUnit.SECONDS).asUtf8String());
    }

    private String download(S3TransferManager manager) throws Exception {
        return manager.download(software.amazon.awssdk.transfer.s3.model.DownloadRequest.builder()
                .getObjectRequest(o -> o.bucket("bucket").key("key"))
                .responseTransformer(AsyncResponseTransformer.toBytes()).build())
                .completionFuture().get(10, TimeUnit.SECONDS).result().asUtf8String();
    }

    @AfterAll
    static void stopServers() {
        archiveServer.stop(0);
        otherServer.stop(0);
    }

    private static HttpServer server(String text) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = text.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("ETag", "\"etag\"");
                exchange.getResponseHeaders().set("Content-Length", Integer.toString(body.length));
                exchange.sendResponseHeaders(200, exchange.getRequestMethod().equals("HEAD") ? -1 : body.length);
                if (!exchange.getRequestMethod().equals("HEAD")) {
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @jakarta.enterprise.context.ApplicationScoped
    public static class CustomProducer {
        private S3TransferManager manager;
        private int created;

        @jakarta.enterprise.inject.Produces
        @jakarta.enterprise.context.ApplicationScoped
        @S3Crt
        @AmazonClient("archive")
        S3TransferManager archive(@S3Crt @AmazonClient("archive") software.amazon.awssdk.services.s3.S3AsyncClient client) {
            created++;
            return manager = S3TransferManager.builder().s3Client(client).build();
        }

        public int created() {
            return created;
        }

        @jakarta.annotation.PreDestroy
        void close() {
            if (manager != null)
                manager.close();
        }
    }
}
