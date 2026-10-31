package io.quarkiverse.amazon.sdk.deployment;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.core.util.VersionInfo;

public class SdkCrtAsyncClientVertxContextTest extends AbstractSdkAsyncClientVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure()
            .setForcedDependencies(List.of(Dependency.of("software.amazon.awssdk", "aws-crt-client", VersionInfo.SDK_VERSION)))
            .overrideConfigKey("quarkus.aws.async-client.type", "aws-crt");
}
