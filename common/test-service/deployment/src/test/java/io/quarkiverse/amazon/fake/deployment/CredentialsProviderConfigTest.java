package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.test.AbstractCredentialsProviderConfigTest;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class CredentialsProviderConfigTest extends AbstractCredentialsProviderConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure(new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot(jar -> jar.addClasses(CredentialsProviderConfigTest.class, SdkHttpClients.class,
                    AbstractCredentialsProviderConfigTest.class, AbstractCredentialsProviderConfigTest.CustomProvider.class)),
            "quarkus.fake-service", ".aws");

    @Inject
    FakeClient defaultClient;
    @Inject
    @AmazonClient("profile")
    FakeClient profileClient;
    @Inject
    @AmazonClient("static")
    FakeAsyncClient staticClient;
    @Inject
    @AmazonClient("custom")
    FakeClient customClient;

    @Override
    protected AwsClient[] clients() {
        return new AwsClient[] { defaultClient, profileClient, staticClient, customClient };
    }

    @Override
    protected void callCustomClient() {
        customClient.listThings();
    }
}
