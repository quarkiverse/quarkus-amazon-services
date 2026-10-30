package io.quarkiverse.amazon.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
public abstract class AbstractClientLifecycleTest {
    private static final String PREFIX = "test.amazon-sdk.lifecycle.";
    private static final String[] CLIENTS = { "sync", "async", "named-sync", "named-async" };

    public static void addToArchive(JavaArchive jar) {
        jar.addClasses(AbstractClientLifecycleTest.class, SyncTransport.class, AsyncTransport.class);
    }

    public static void assertClosed() {
        try {
            for (String client : CLIENTS) {
                assertEquals("closed", System.getProperty(PREFIX + client), client);
            }
        } finally {
            for (String client : CLIENTS) {
                System.clearProperty(PREFIX + client);
            }
        }
    }

    protected abstract AwsSyncClientBuilder<?, ?>[] syncBuilders();

    protected abstract AwsAsyncClientBuilder<?, ?>[] asyncBuilders();

    protected abstract AwsClient[] clients();

    @Test
    public void cdiDestructionClosesAllClientsAndTheirOwnedTransports() {
        // Supply builders, not client instances: the SDK must own and close each transport.
        syncBuilders()[0].httpClientBuilder(new SyncTransport("sync"));
        asyncBuilders()[0].httpClientBuilder(new AsyncTransport("async"));
        syncBuilders()[1].httpClientBuilder(new SyncTransport("named-sync"));
        asyncBuilders()[1].httpClientBuilder(new AsyncTransport("named-async"));
        for (AwsClient client : clients()) {
            client.serviceClientConfiguration();
        }
        for (String client : CLIENTS) {
            assertEquals("created", System.getProperty(PREFIX + client), client);
        }
    }

    static class SyncTransport implements SdkHttpClient.Builder<SyncTransport> {
        private final String name;

        SyncTransport(String name) {
            this.name = name;
        }

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap defaults) {
            SdkHttpClient delegate = UrlConnectionHttpClient.builder().buildWithDefaults(defaults);
            System.setProperty(PREFIX + name, "created");
            return new SdkHttpClient() {
                @Override
                public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                    return delegate.prepareRequest(request);
                }

                @Override
                public void close() {
                    delegate.close();
                    System.setProperty(PREFIX + name, "closed");
                }
            };
        }
    }

    static class AsyncTransport implements SdkAsyncHttpClient.Builder<AsyncTransport> {
        private final String name;

        AsyncTransport(String name) {
            this.name = name;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap defaults) {
            SdkAsyncHttpClient delegate = NettyNioAsyncHttpClient.builder().buildWithDefaults(defaults);
            System.setProperty(PREFIX + name, "created");
            return new SdkAsyncHttpClient() {
                @Override
                public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
                    return delegate.execute(request);
                }

                @Override
                public void close() {
                    delegate.close();
                    System.setProperty(PREFIX + name, "closed");
                }
            };
        }
    }
}
