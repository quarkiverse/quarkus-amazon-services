package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractLifecycleResource;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;
import software.amazon.awssdk.core.util.VersionInfo;

/**
 * Verifies production packaging fails when the application does not provide the configured URLConnection transport dependency.
 */
class MissingUrlConnectionClientTest {
    @RegisterExtension
    static final QuarkusProdModeTest app = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(LifecycleResource.class, AbstractLifecycleResource.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-rest", Version.getVersion()),
                    Dependency.of("software.amazon.awssdk", "ecr", VersionInfo.SDK_VERSION)))
            .overrideConfigKey("quarkus.aws.region", "us-east-1")
            .assertBuildException(failure -> {
                while (failure.getCause() != null) {
                    failure = failure.getCause();
                }
                assertEquals("Missing 'software.amazon.awssdk:url-connection-client' dependency on the classpath",
                        failure.getMessage());
            });

    @Test
    void requiresTheConfiguredTransportInTheApplication() {
        fail("Augmentation must fail without the configured URLConnection transport");
    }
}
