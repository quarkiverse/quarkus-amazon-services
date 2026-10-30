package io.quarkiverse.amazon.sdk.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractDevServicesProductionTest;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;
import software.amazon.awssdk.core.util.VersionInfo;

/** Runs the same production scenario as the historical fake-service DevServicesProductionTest. */
class DevServicesProductionTest extends AbstractDevServicesProductionTest {
    @RegisterExtension
    static final QuarkusProdModeTest app = configure(new QuarkusProdModeTest(), "quarkus.aws", "", LifecycleResource.class,
            Dependency.of("software.amazon.awssdk", "ecr", VersionInfo.SDK_VERSION));
}
