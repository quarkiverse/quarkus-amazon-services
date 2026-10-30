package io.quarkiverse.amazon.fake.deployment;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractDevServicesReloadTest;
import io.quarkiverse.amazon.common.test.AbstractLifecycleResource;
import io.quarkus.test.QuarkusDevModeTest;

/** Runs the same real reload scenario as the generic SDK DevServicesReloadTest. */
@EnabledIfSystemProperty(named = "test.aws.devservices.container", matches = "true")
class DevServicesReloadTest extends AbstractDevServicesReloadTest {
    @RegisterExtension
    static final QuarkusDevModeTest app = configure(new QuarkusDevModeTest(), "quarkus.fake-service", ".aws",
            LifecycleResource.class);

    @Override
    protected QuarkusDevModeTest application() {
        return app;
    }

    @Override
    protected Class<? extends AbstractLifecycleResource> resourceClass() {
        return LifecycleResource.class;
    }

    @Override
    protected String prefix() {
        return "quarkus.fake-service";
    }
}
