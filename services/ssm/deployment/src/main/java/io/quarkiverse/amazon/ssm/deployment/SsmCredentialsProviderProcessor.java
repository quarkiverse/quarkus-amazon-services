package io.quarkiverse.amazon.ssm.deployment;

import io.quarkiverse.amazon.ssm.runtime.SsmCredentialsProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class SsmCredentialsProviderProcessor {
    @BuildStep
    AdditionalBeanBuildItem credentialsProvider() {
        return AdditionalBeanBuildItem.unremovableOf(SsmCredentialsProvider.class);
    }
}
