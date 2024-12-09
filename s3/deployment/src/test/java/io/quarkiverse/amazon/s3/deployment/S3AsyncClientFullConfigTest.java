package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

public class S3AsyncClientFullConfigTest {

    @Inject
    Instance<S3AsyncClient> client;

    @Inject
    @AmazonClient("custom")
    Instance<S3AsyncClient> clientCustom;

    @Inject
    @Any
    Instance<S3AsyncClientBuilder> builder;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("async-netty-full-config.properties", "application.properties"));

    @Test
    public void test() {
        //assertNotNull(client.get());
        assertNotNull(clientCustom.get());

        // should finish with success
        client.get().close();
        clientCustom.get().close();

    }
}
