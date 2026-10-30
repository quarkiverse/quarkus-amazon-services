package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.deployment.AmazonClientOwnershipBuildItem;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.EcrClientBuilder;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

/**
 * Verifies Jandex-based client and builder discovery without SDK class loading, and dedicated extension precedence.
 */
class ServiceDiscoveryTest {
    @Test
    void defersToDedicatedExtension() throws Exception {
        Index index = Index.of(EcrClient.class, EcrAsyncClient.class, EcrClientBuilder.class, SesV2Client.class,
                SesV2ClientBuilder.class, AwsSyncClientBuilder.class);
        List<AmazonSdkServiceBuildItem> services = new ArrayList<>();

        new AmazonSdkProcessor().discover(new AmazonSdkIndexBuildItem(index),
                List.of(new AmazonClientOwnershipBuildItem(DotName.createSimple(SesV2Client.class.getName()),
                        DotName.createSimple(SesV2AsyncClient.class.getName()))),
                services::add);

        assertEquals(List.of("ecr"), services.stream().map(s -> s.service).toList());
    }

    @Test
    void discoversTypesAndFactoriesWithoutLoadingSdkClasses() throws Exception {
        Index index = Index.of(EcrClient.class, EcrAsyncClient.class, EcrClientBuilder.class, AwsSyncClientBuilder.class);
        List<AmazonSdkServiceBuildItem> services = new ArrayList<>();
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ClassLoader(original) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("software.amazon.awssdk.services.")) {
                    throw new AssertionError("Discovery must use Jandex, not load " + name);
                }
                return super.loadClass(name, resolve);
            }
        });
        try {
            new AmazonSdkProcessor().discover(new AmazonSdkIndexBuildItem(index), List.of(), services::add);
        } finally {
            thread.setContextClassLoader(original);
        }
        assertEquals(1, services.size());
        assertEquals(DotName.createSimple(EcrClient.class.getName()), services.getFirst().syncClient);
        assertEquals(DotName.createSimple(EcrAsyncClient.class.getName()), services.getFirst().asyncClient);
        assertEquals(DotName.createSimple(EcrClientBuilder.class.getName()), services.getFirst().syncBuilder);
        assertEquals(DotName.createSimple("software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder"),
                services.getFirst().asyncBuilder);
    }
}
