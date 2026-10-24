package io.quarkiverse.amazon.ecr.test;

import io.quarkiverse.amazon.ecr.deployment.EcrTestProcessor;
import io.quarkiverse.amazon.ecr.runtime.EcrSyntheticBean;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.VersionInfo;

@Generated("io.quarkiverse.amazon:codegen")
public class EcrSyntheticBeanTest {
    @RegisterExtension
    static final QuarkusExtensionTest extension = new QuarkusExtensionTest().setForcedDependencies(
            List.of(Dependency.of("software.amazon.awssdk", "url-connection-client", VersionInfo.SDK_VERSION),
                    Dependency.of("software.amazon.awssdk", "netty-nio-client", VersionInfo.SDK_VERSION))).setArchiveProducer(
            () -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(EcrTestProcessor.class)
                    .addAsResource(new StringAsset("io.quarkiverse.amazon.ecr.deployment.EcrTestProcessor"),
                            "META-INF/quarkus-build-steps.list")
                    .addAsResource(
                            new StringAsset("quarkus.ecr.aws.credentials.type=static\n"
                                    + "quarkus.ecr.aws.credentials.static-provider.secret-access-key=test-secret\n"
                                    + "quarkus.ecr.aws.region=us-east-2\n"
                                    + "quarkus.ecr.endpoint-override=http://localhost:9090\n"
                                    + "quarkus.ecr.aws.credentials.static-provider.access-key-id=test-key"),
                            "application.properties"));

    @Inject
    EcrSyntheticBean bean;

    public EcrSyntheticBeanTest() {
    }

    @Test
    public void fullConfig() {
        Assertions.assertNotNull(bean);
        Assertions.assertDoesNotThrow(() -> {
            bean.invokeAsyncClient();
        });
        Assertions.assertDoesNotThrow(() -> {
            bean.invokeSyncClient();
        });
    }
}