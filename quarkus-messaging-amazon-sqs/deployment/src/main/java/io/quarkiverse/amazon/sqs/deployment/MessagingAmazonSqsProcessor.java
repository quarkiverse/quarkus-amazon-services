package io.quarkiverse.amazon.sqs.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Processor
 */
public class MessagingAmazonSqsProcessor {

    private static final String FEATURE = "messaging-amazon-sqs";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
