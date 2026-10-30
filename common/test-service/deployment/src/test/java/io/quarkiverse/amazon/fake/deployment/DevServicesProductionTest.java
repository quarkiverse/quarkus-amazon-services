package io.quarkiverse.amazon.fake.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractDevServicesProductionTest;
import io.quarkus.test.QuarkusProdModeTest;

/** Runs the same production scenario as the generic SDK DevServicesProductionTest. */
class DevServicesProductionTest extends AbstractDevServicesProductionTest {
    @RegisterExtension
    static final QuarkusProdModeTest app = configure(new QuarkusProdModeTest(), "quarkus.fake-service", ".aws",
            LifecycleResource.class);
}
