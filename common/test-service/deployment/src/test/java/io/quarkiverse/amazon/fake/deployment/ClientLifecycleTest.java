package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.AmazonClientBuilder;
import io.quarkiverse.amazon.common.test.AbstractClientLifecycleTest;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeAsyncClientBuilder;
import software.amazon.awssdk.services.fake.FakeClient;
import software.amazon.awssdk.services.fake.FakeClientBuilder;

class ClientLifecycleTest extends AbstractClientLifecycleTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot(jar -> {
                jar.addClasses(ClientLifecycleTest.class, SdkHttpClients.class);
                addToArchive(jar);
            })
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "anonymous")
            .setAfterUndeployListener(AbstractClientLifecycleTest::assertClosed);

    @Inject
    FakeClient sync;
    @Inject
    FakeAsyncClient async;
    @Inject
    @AmazonClient("archive")
    FakeClient namedSync;
    @Inject
    @AmazonClient("archive")
    FakeAsyncClient namedAsync;
    @Inject
    FakeClientBuilder syncBuilder;
    @Inject
    FakeAsyncClientBuilder asyncBuilder;
    @Inject
    @AmazonClientBuilder("archive")
    FakeClientBuilder namedSyncBuilder;
    @Inject
    @AmazonClientBuilder("archive")
    FakeAsyncClientBuilder namedAsyncBuilder;

    @Override
    protected AwsSyncClientBuilder<?, ?>[] syncBuilders() {
        return new AwsSyncClientBuilder<?, ?>[] { syncBuilder, namedSyncBuilder };
    }

    @Override
    protected AwsAsyncClientBuilder<?, ?>[] asyncBuilders() {
        return new AwsAsyncClientBuilder<?, ?>[] { asyncBuilder, namedAsyncBuilder };
    }

    @Override
    protected AwsClient[] clients() {
        return new AwsClient[] { sync, async, namedSync, namedAsync };
    }
}
