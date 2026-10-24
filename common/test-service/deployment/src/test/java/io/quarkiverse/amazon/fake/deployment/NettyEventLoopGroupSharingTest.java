package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.configuration;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

/**
 * Async clients with the same event loop settings share one Netty event loop group instead of each starting a pool.
 */
public class NettyEventLoopGroupSharingTest {

    @Inject
    FakeAsyncClient defaultClient;

    @Inject
    @AmazonClient("other")
    FakeAsyncClient otherClient;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class, FakeClientInspector.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.async-client.type", "netty")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.fake-service.other.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.other.aws.credentials.type", "anonymous");

    @Test
    public void test() {
        Object group = eventLoopGroup(defaultClient);

        assertNotNull(group);
        assertSame(group, eventLoopGroup(otherClient));
    }

    /**
     * The SDK keeps the event loop group in a private field of the Netty client, which it may wrap in a delegating
     * client. A group passed to the client builder is wrapped again in a non-managed group for each client, so the
     * Netty group it delegates to is returned. The Netty client is not on the test compile classpath, so all of this
     * is found by name.
     */
    private static Object eventLoopGroup(FakeAsyncClient client) {
        Object httpClient = configuration(client).option(SdkClientOption.ASYNC_HTTP_CLIENT);
        try {
            while (true) {
                for (Field field : httpClient.getClass().getDeclaredFields()) {
                    if (field.getType().getName().equals("software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup")) {
                        field.setAccessible(true);
                        Object sdkGroup = field.get(httpClient);
                        Object nettyGroup = sdkGroup.getClass().getMethod("eventLoopGroup").invoke(sdkGroup);
                        return nettyGroup.getClass().getSimpleName().equals("NonManagedEventLoopGroup")
                                ? nettyGroup.getClass().getMethod("getDelegate").invoke(nettyGroup)
                                : nettyGroup;
                    }
                }
                Field delegate = httpClient.getClass().getDeclaredField("delegate");
                delegate.setAccessible(true);
                httpClient = delegate.get(httpClient);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No event loop group found in " + httpClient.getClass(), e);
        }
    }
}
