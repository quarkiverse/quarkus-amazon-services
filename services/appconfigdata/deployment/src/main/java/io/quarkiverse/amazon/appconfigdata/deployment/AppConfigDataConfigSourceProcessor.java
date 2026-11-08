package io.quarkiverse.amazon.appconfigdata.deployment;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataConfigSourceBuilder;
import io.quarkiverse.amazon.common.config.AwsConfigSourceCloser;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
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

    @BuildStep
    AdditionalBeanBuildItem appConfigDataConfigSourceCloser() {
        return AdditionalBeanBuildItem.unremovableOf(AwsConfigSourceCloser.class);
    }
}
