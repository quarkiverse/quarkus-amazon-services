package io.quarkiverse.amazon.sdk.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.AmazonClientBuilder;
import io.quarkiverse.amazon.common.test.AbstractClientLifecycleTest;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.EcrClientBuilder;

class ClientLifecycleTest extends AbstractClientLifecycleTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> {
                jar.addClass(ClientLifecycleTest.class);
                addToArchive(jar);
            })
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.aws.credentials.type", "anonymous")
            .setAfterUndeployListener(AbstractClientLifecycleTest::assertClosed);

    @Inject
    EcrClient sync;
    @Inject
    EcrAsyncClient async;
    @Inject
    @AmazonClient("archive")
    EcrClient namedSync;
    @Inject
    @AmazonClient("archive")
    EcrAsyncClient namedAsync;
    @Inject
    EcrClientBuilder syncBuilder;
    @Inject
    EcrAsyncClientBuilder asyncBuilder;
    @Inject
    @AmazonClientBuilder("archive")
    EcrClientBuilder namedSyncBuilder;
    @Inject
    @AmazonClientBuilder("archive")
    EcrAsyncClientBuilder namedAsyncBuilder;

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
