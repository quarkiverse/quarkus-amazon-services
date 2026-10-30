package io.quarkiverse.amazon.sdk.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.test.AbstractCredentialsProviderConfigTest;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

class CredentialsProviderConfigTest extends AbstractCredentialsProviderConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure(new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(CredentialsProviderConfigTest.class, AbstractCredentialsProviderConfigTest.class,
                            AbstractCredentialsProviderConfigTest.CustomProvider.class)),
            "quarkus.aws", "");

    @Inject
    EcrClient defaultClient;
    @Inject
    @AmazonClient("profile")
    EcrClient profileClient;
    @Inject
    @AmazonClient("static")
    EcrAsyncClient staticClient;
    @Inject
    @AmazonClient("custom")
    EcrClient customClient;

    @Override
    protected AwsClient[] clients() {
        return new AwsClient[] { defaultClient, profileClient, staticClient, customClient };
    }

    @Override
    protected void callCustomClient() {
        customClient.describeRepositories();
    }
}
