package io.quarkus.amazon.s3.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.s3.S3Client;

class S3CustomCredentialsProviderMissedBeanTest {

    @Inject
    Instance<S3Client> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException(throwable -> {
                assertThat(throwable.getMessage(),
                        equalTo("cannot find bean 'missed-bean' specified in quarkus.s3.aws.credentials.custom-provider.name"));
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("custom-credentials-provider-missed-bean.properties", "application.properties"));

    @Test
    void test() {
        Assertions.fail();
    }

}
