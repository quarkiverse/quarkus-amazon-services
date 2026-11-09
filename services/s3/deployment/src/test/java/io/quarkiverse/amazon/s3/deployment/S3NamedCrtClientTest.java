package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.AmazonClientBuilder;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

class S3NamedCrtClientTest {
    @Inject
    CustomProducer producer;
    @Inject
    @S3Crt
    S3AsyncClient defaultCrt;
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(CustomProducer.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.s3.crt-client.max-concurrency", "10")
            .overrideConfigKey("quarkus.s3.crt-client.target-throughput-in-gbps", "1")
            .overrideConfigKey("quarkus.s3.\"archive\".crt-client.max-concurrency", "20")
            .overrideConfigKey("quarkus.s3.\"archive\".aws.region", "eu-west-1");

    @Inject
    @AmazonClient("archive")
    S3AsyncClient standard;
    @Inject
    @S3Crt
    @AmazonClient("archive")
    S3AsyncClient archive;
    @Inject
    @S3Crt
    @AmazonClient("inherited")
    S3AsyncClient inherited;
    @Inject
    @AmazonClientBuilder("archive")
    S3CrtAsyncClientBuilder archiveBuilder;
    @Inject
    @AmazonClientBuilder("inherited")
    S3CrtAsyncClientBuilder inheritedBuilder;

    @Test
    void namedNativeAndStandardClientsCoexist() {
        assertNotSame(ClientProxy.unwrap(defaultCrt), ClientProxy.unwrap(archive));
        assertInstanceOf(S3CrtAsyncClient.class, ClientProxy.unwrap(archive));
        assertInstanceOf(S3CrtAsyncClient.class, ClientProxy.unwrap(inherited));
        assertFalse(ClientProxy.unwrap(standard) instanceof S3CrtAsyncClient);
        assertNotSame(ClientProxy.unwrap(archive), ClientProxy.unwrap(inherited));
        assertEquals(1, producer.created());
    }

    @Test
    void crtSettingsFallBackPerProperty() throws Exception {
        assertEquals(20, field(archiveBuilder, "maxConcurrency"));
        assertEquals(10, field(inheritedBuilder, "maxConcurrency"));
        assertEquals(1.0, field(archiveBuilder, "targetThroughputInGbps"));
        assertEquals("eu-west-1", field(archiveBuilder, "region").toString());
        assertEquals("us-east-1", field(inheritedBuilder, "region").toString());
    }

    private Object field(S3CrtAsyncClientBuilder proxy, String name) throws Exception {
        Object builder = ClientProxy.unwrap(proxy);
        var field = builder.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(builder);
    }

    @jakarta.enterprise.context.ApplicationScoped
    public static class CustomProducer {
        private S3AsyncClient client;
        private int created;

        @jakarta.enterprise.inject.Produces
        @jakarta.enterprise.context.ApplicationScoped
        @S3Crt
        @AmazonClient("archive")
        S3AsyncClient archive(@AmazonClientBuilder("archive") S3CrtAsyncClientBuilder builder) {
            created++;
            return client = builder.build();
        }

        public int created() {
            return created;
        }

        @jakarta.annotation.PreDestroy
        void close() {
            if (client != null)
                client.close();
        }
    }
}
