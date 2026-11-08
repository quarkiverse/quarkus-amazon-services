package io.quarkiverse.amazon.s3.deployment;

import io.quarkiverse.amazon.common.config.AwsConfigSourceCloser;
import io.quarkiverse.amazon.s3.runtime.S3ConfigSourceBuilder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class S3ConfigSourceProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("aws-s3-config-source");
    }

    @BuildStep
    RunTimeConfigBuilderBuildItem configSource() {
        return new RunTimeConfigBuilderBuildItem(S3ConfigSourceBuilder.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem closer() {
        return AdditionalBeanBuildItem.unremovableOf(AwsConfigSourceCloser.class);
    }
}
