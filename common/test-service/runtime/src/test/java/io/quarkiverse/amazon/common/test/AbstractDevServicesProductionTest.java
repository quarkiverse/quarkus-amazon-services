package io.quarkiverse.amazon.common.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.QuarkusProdModeTest;
import software.amazon.awssdk.core.util.VersionInfo;

/** Verifies both extension paths respect the production launch-mode guard and never inject emulator endpoints. */
public abstract class AbstractDevServicesProductionTest {
    public static QuarkusProdModeTest configure(QuarkusProdModeTest app, String prefix, String awsSuffix,
            Class<? extends AbstractLifecycleResource> resource, Dependency... sdkDependencies) {
        var dependencies = new ArrayList<>(List.of(
                Dependency.of("io.quarkus", "quarkus-rest", Version.getVersion()),
                Dependency.of("software.amazon.awssdk", "url-connection-client", VersionInfo.SDK_VERSION),
                Dependency.of("software.amazon.awssdk", "netty-nio-client", VersionInfo.SDK_VERSION)));
        dependencies.addAll(List.of(sdkDependencies));
        return app.withApplicationRoot(jar -> jar.addClasses(resource, AbstractLifecycleResource.class))
                .setForcedDependencies(dependencies)
                .overrideConfigKey(prefix + awsSuffix + ".region", "us-east-1")
                .overrideConfigKey(prefix + ".devservices.enabled", "true")
                .addBuildChainCustomizerEntries(new QuarkusProdModeTest.BuildChainCustomizerEntry(
                        NoDevServices.class, List.of(ServiceStartBuildItem.class), List.of(DevServicesResultBuildItem.class)))
                .setRun(true);
    }

    @Test
    public void doesNotInjectEmulatorEndpointsInProduction() {
        given().get("/lifecycle/endpoints").then().statusCode(200).body(is("aws|aws"));
    }

    public static class NoDevServices extends ProdModeTestBuildStep {
        public NoDevServices(Map<String, Object> context) {
            super(context);
        }

        @Override
        public void execute(BuildContext context) {
            if (!context.consumeMulti(DevServicesResultBuildItem.class).isEmpty()) {
                throw new IllegalStateException("Production augmentation must not request any Dev Service");
            }
            context.produce(new ServiceStartBuildItem("no-aws-devservices"));
        }
    }
}
