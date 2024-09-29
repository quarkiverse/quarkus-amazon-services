package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3DevServicesTest {

    @Inject
    Instance<S3Client> client;

    @Inject
    @AmazonClient("test")
    Instance<S3Presigner> clientNamed;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void test() {
        assertNotNull(clientNamed.get());
        assertNotNull(client.get());
        Assertions.assertEquals(Set.of("default"),
                client.get().listBuckets().buckets().stream().map(Bucket::name).collect(Collectors.toSet()));
    }
}
