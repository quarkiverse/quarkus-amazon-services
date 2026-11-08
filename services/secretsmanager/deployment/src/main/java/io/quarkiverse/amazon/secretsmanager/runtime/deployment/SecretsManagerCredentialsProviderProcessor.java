package io.quarkiverse.amazon.secretsmanager.runtime.deployment;

import io.quarkiverse.amazon.secretsmanager.runtime.SecretsManagerCredentialsProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class SecretsManagerCredentialsProviderProcessor {
    @BuildStep
    AdditionalBeanBuildItem credentialsProvider() {
        return AdditionalBeanBuildItem.unremovableOf(SecretsManagerCredentialsProvider.class);
    }
}
