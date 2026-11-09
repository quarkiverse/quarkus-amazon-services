package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3NamedConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.s3.path-style-access", "true")
            .overrideConfigKey("quarkus.s3.endpoint-override", "http://localhost:9090")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "static")
            .overrideConfigKey("quarkus.s3.aws.credentials.static-provider.access-key-id", "test-key")
            .overrideConfigKey("quarkus.s3.aws.credentials.static-provider.secret-access-key", "test-secret")
            .overrideConfigKey("quarkus.s3.\"archive\".path-style-access", "false")
            .overrideConfigKey("quarkus.s3.\"archive\".aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.s3.\"inherited\".aws.region", "eu-west-2");

    @Inject
    S3Client defaultClient;
    @Inject
    @AmazonClient("archive")
    S3Client archive;
    @Inject
    @AmazonClient("archive")
    S3AsyncClient archiveAsync;
    @Inject
    @AmazonClient("inherited")
    S3Client inherited;
    @Inject
    @AmazonClient("inherited")
    S3AsyncClient inheritedAsync;
    @Inject
    @AmazonClient("archive")
    S3Presigner archivePresigner;
    @Inject
    @AmazonClient("inherited")
    S3Presigner inheritedPresigner;

    @Test
    void namedClientsOverrideOrInheritS3SettingsIndependentlyOfAwsSettings() {
        assertEquals("http://localhost:9090/bucket/key",
                defaultClient.utilities().getUrl(r -> r.bucket("bucket").key("key")).toString());
        assertEquals("http://bucket.localhost:9090/key",
                archive.utilities().getUrl(r -> r.bucket("bucket").key("key")).toString());
        assertEquals("http://bucket.localhost:9090/key",
                archiveAsync.utilities().getUrl(r -> r.bucket("bucket").key("key")).toString());
        assertEquals("http://localhost:9090/bucket/key",
                inherited.utilities().getUrl(r -> r.bucket("bucket").key("key")).toString());
        assertEquals("http://localhost:9090/bucket/key",
                inheritedAsync.utilities().getUrl(r -> r.bucket("bucket").key("key")).toString());
        assertEquals("eu-west-1", archive.serviceClientConfiguration().region().id());
        assertEquals("eu-west-2", inherited.serviceClientConfiguration().region().id());
    }

    @Test
    void namedPresignersUseTheSameFallback() throws Exception {
        URI archiveUrl = archivePresigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(o -> o.bucket("bucket").key("key"))).url().toURI();
        URI inheritedUrl = inheritedPresigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(o -> o.bucket("bucket").key("key"))).url().toURI();
        assertEquals("bucket.localhost", archiveUrl.getHost());
        assertEquals("/key", archiveUrl.getPath());
        assertEquals("localhost", inheritedUrl.getHost());
        assertEquals("/bucket/key", inheritedUrl.getPath());
    }
}
