package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedClient;

class DynamoDbEnhancedNamedExtensionsTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RootExtension.class, NamedExtension.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.dynamodb.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.dynamodb.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.dynamodbenhanced.client-extensions", RootExtension.class.getName())
            .overrideConfigKey("quarkus.dynamodbenhanced.\"archive\".client-extensions", NamedExtension.class.getName());

    @Inject
    DynamoDbEnhancedClient root;
    @Inject
    DynamoDbEnhancedAsyncClient rootAsync;
    @Inject
    @AmazonClient("archive")
    DynamoDbEnhancedClient archive;
    @Inject
    @AmazonClient("archive")
    DynamoDbEnhancedAsyncClient archiveAsync;
    @Inject
    @AmazonClient("inherited")
    DynamoDbEnhancedClient inherited;
    @Inject
    @AmazonClient("inherited")
    DynamoDbEnhancedAsyncClient inheritedAsync;

    @Test
    void extensionsAreSelectedPerNameForBothClientTypes() {
        assertInstanceOf(RootExtension.class, extension(root));
        assertInstanceOf(RootExtension.class, extension(rootAsync));
        assertInstanceOf(NamedExtension.class, extension(archive));
        assertInstanceOf(NamedExtension.class, extension(archiveAsync));
        assertInstanceOf(RootExtension.class, extension(inherited));
        assertInstanceOf(RootExtension.class, extension(inheritedAsync));
    }

    @Test
    void syncAndAsyncClientsOfTheSameNameShareTheirExtensionInstance() {
        assertSame(extension(root), extension(rootAsync));
        assertSame(extension(archive), extension(archiveAsync));
        assertSame(extension(inherited), extension(inheritedAsync));
    }

    private DynamoDbEnhancedClientExtension extension(DynamoDbEnhancedClient client) {
        return ((DefaultDynamoDbEnhancedClient) ClientProxy.unwrap(client)).mapperExtension();
    }

    private DynamoDbEnhancedClientExtension extension(DynamoDbEnhancedAsyncClient client) {
        return ((DefaultDynamoDbEnhancedAsyncClient) ClientProxy.unwrap(client)).mapperExtension();
    }

    public static class RootExtension implements DynamoDbEnhancedClientExtension {
    }

    public static class NamedExtension implements DynamoDbEnhancedClientExtension {
    }
}
