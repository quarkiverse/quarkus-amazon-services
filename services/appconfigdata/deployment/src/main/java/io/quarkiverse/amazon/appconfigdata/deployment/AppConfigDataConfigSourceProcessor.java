package io.quarkiverse.amazon.appconfigdata.deployment;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataConfigSourceBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class AppConfigDataConfigSourceProcessor {

    static final String FEATURE = "aws-appconfig-data-config-source";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void awsAppConfigDataConfigFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(AppConfigDataConfigSourceBuilder.class.getName()));
    }
}
