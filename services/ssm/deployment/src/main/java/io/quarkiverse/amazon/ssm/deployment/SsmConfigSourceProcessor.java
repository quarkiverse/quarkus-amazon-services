package io.quarkiverse.amazon.ssm.deployment;

import io.quarkiverse.amazon.ssm.runtime.SsmConfigSourceBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class SsmConfigSourceProcessor {

    static final String FEATURE = "aws-ssm-parameter-store-config-source";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void ssmConfigSourceFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(SsmConfigSourceBuilder.class.getName()));
    }
}
