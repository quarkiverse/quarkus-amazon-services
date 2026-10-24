package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The S3 specific configuration is applied to the clients.
 */
public class S3ConfigTest {

    @Inject
    S3Client sync;

    @Inject
    S3AsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.s3.path-style-access", "true")
            .overrideConfigKey("quarkus.s3.endpoint-override", "http://localhost:9090")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "static")
            .overrideConfigKey("quarkus.s3.aws.credentials.static-provider.access-key-id", "test-key")
            .overrideConfigKey("quarkus.s3.aws.credentials.static-provider.secret-access-key", "test-secret");

    @Test
    public void test() {
        assertEquals("http://localhost:9090/bucket/key",
                sync.utilities().getUrl(b -> b.bucket("bucket").key("key")).toString());
        assertEquals("http://localhost:9090/bucket/key",
                async.utilities().getUrl(b -> b.bucket("bucket").key("key")).toString());
    }
}
