package io.quarkus.amazon.s3.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkus.test.QuarkusUnitTest;

class S3CustomCredentialsProviderMissedBeanTest {

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
