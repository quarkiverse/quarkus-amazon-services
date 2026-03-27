package io.quarkiverse.amazon.secretsmanager.runtime.deployment;

import io.quarkiverse.amazon.secretsmanager.runtime.SecretsManagerConfigSourceBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class SecretsManagerConfigSourceProcessor {

    static final String FEATURE = "aws-secrets-manager-config-source";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void awsSecretsManagerConfigFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(SecretsManagerConfigSourceBuilder.class.getName()));
    }
}
