package io.quarkiverse.amazon.s3.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3Client;

class S3CustomCredentialsProviderMissedBeanNameNamedClientTest {

    @Inject
    @AmazonClient("custom")
    Instance<S3Client> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(throwable -> {
                assertThat(throwable.getMessage(),
                        equalTo("quarkus.s3.custom.aws.credentials.custom-provider.name cannot be empty if CUSTOM credentials provider used."));
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("custom-credentials-provider-missed-name-named-client.properties",
                            "application.properties"));

    @Test
    void test() {
        Assertions.fail();
    }

}
