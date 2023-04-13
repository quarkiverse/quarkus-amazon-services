package io.quarkus.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

public class S3DevServicesTest {

    @Inject
    Instance<S3Client> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.aws.devservices.localstack.image-name=localstack/localstack:1.4.0"),
                            "application.properties"));

    @Test
    public void test() {
        assertNotNull(client.get());
        Assertions.assertEquals(Set.of("default"),
                client.get().listBuckets().buckets().stream().map(Bucket::name).collect(Collectors.toSet()));
    }
}
