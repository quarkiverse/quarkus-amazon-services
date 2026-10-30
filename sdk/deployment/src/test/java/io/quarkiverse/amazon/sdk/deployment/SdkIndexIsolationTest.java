package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.ecr.EcrClient;

/**
 * Verifies SDK classes stay out of the combined index while application interceptors remain indexed and applied to clients.
 */
class SdkIndexIsolationTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(SdkIndexIsolationTest.class, ApplicationInterceptor.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.aws.interceptors", ApplicationInterceptor.class.getName())
            .addBuildChainCustomizer(chain -> chain.addBuildStep(context -> {
                var index = context.consume(CombinedIndexBuildItem.class).getIndex();
                assertNotNull(index.getClassByName(DotName.createSimple(ApplicationInterceptor.class.getName())),
                        "Application interceptors must remain in the combined index");
                assertNull(index.getClassByName(DotName.createSimple(EcrClient.class.getName())),
                        "SDK service discovery must not add service jars to the combined index");
                assertNull(index.getClassByName(DotName.createSimple(
                        "software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor")),
                        "SDK internal interceptors must stay out of the combined index");
                context.produce(new ServiceStartBuildItem("sdk-index-isolation"));
            }).consumes(CombinedIndexBuildItem.class).produces(ServiceStartBuildItem.class).build());

    @Inject
    EcrClient client;

    @Test
    void discoversSdkClientsWithoutExposingSdkInternalsToApplicationIndexLookups() {
        assertTrue(client.serviceClientConfiguration().overrideConfiguration().executionInterceptors().stream()
                .anyMatch(ApplicationInterceptor.class::isInstance),
                "The discovered client must still use the application's interceptor");
    }

    public static class ApplicationInterceptor implements ExecutionInterceptor {
    }
}
