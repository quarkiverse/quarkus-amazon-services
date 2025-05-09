package io.quarkiverse.amazon.ssooidc.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class SsoOidcNativeProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        Stream.of(
                "software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }
}
