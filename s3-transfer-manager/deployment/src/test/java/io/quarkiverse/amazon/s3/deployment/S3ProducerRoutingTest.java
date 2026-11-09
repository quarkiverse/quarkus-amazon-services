package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

class S3ProducerRoutingTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(CustomProducers.class, ApplicationS3Client.class,
                    ApplicationTransferManager.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "anonymous");

    @Inject
    CustomProducers producers;
    @Inject
    S3AsyncClient defaultClient;
    @Inject
    @AmazonClient("output")
    S3TransferManager namedManager;
    @Inject
    @AmazonClient("client-subinterface")
    S3AsyncClient subtypeClient;
    @Inject
    @AmazonClient("manager-subinterface")
    S3TransferManager subtypeManager;

    @Test
    void parameterQualifiersDoNotChangeTheDefaultProducer() {
        assertSame(producers.defaultClient, defaultClient);
    }

    @Test
    void parameterNameAndVariantDoNotChangeTheNamedProducer() {
        assertSame(producers.namedManager, namedManager);
    }

    @Test
    void clientSubinterfaceProducerOverridesTheSdkInterface() {
        assertSame(producers.subtypeClient, subtypeClient);
    }

    @Test
    void managerSubinterfaceProducerOverridesTheSdkInterface() {
        assertSame(producers.subtypeManager, subtypeManager);
    }

    public interface ApplicationS3Client extends S3AsyncClient {
    }

    public interface ApplicationTransferManager extends S3TransferManager {
    }

    @Singleton
    public static class CustomProducers {
        final S3AsyncClient defaultClient = stub(S3AsyncClient.class);
        final S3TransferManager namedManager = stub(S3TransferManager.class);
        final ApplicationTransferManager subtypeManager = stub(ApplicationTransferManager.class);

        @Produces
        @Singleton
        @S3Crt
        @AmazonClient("input")
        S3AsyncClient input = stub(S3AsyncClient.class);

        @Produces
        @Singleton
        S3AsyncClient defaultClient(@S3Crt @AmazonClient("input") S3AsyncClient input) {
            return defaultClient;
        }

        @Produces
        @Singleton
        @AmazonClient("output")
        S3TransferManager namedManager(@S3Crt @AmazonClient("input") S3AsyncClient input) {
            return namedManager;
        }

        @Produces
        @Singleton
        @AmazonClient("client-subinterface")
        ApplicationS3Client subtypeClient = stub(ApplicationS3Client.class);

        @Produces
        @Singleton
        @AmazonClient("manager-subinterface")
        ApplicationTransferManager subtypeManager() {
            return subtypeManager;
        }

        private static <T> T stub(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            }));
        }
    }
}
