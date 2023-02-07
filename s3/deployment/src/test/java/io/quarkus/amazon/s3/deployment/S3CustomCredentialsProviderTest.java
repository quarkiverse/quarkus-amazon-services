package io.quarkus.amazon.s3.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;

class S3CustomCredentialsProviderTest {

    @Inject
    Instance<S3Client> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("custom-credentials-provider.properties", "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        SdkClientException expected = assertThrows(SdkClientException.class, () -> {
            client.get().listBuckets();
        });

        assertThat(expected.getMessage(),
                startsWith("Profile file contained no credentials for profile 'expected-not-exist'"));
    }

    @Named("test-creds-profile")
    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider provider() {
        return ProfileCredentialsProvider.create("expected-not-exist");
    }
}
