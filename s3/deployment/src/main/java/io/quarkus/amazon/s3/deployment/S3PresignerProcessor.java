package io.quarkus.amazon.s3.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkus.amazon.common.runtime.ClientUtil;
import io.quarkus.amazon.s3.runtime.S3PresignerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * This processor ensures backward compatibility for applications using `S3Presigner`, such as `camel-quarkus-aws2-s3`.
 * Previously, `S3Presigner` was produced even when no injection points were discovered.
 * With support for multiple named clients, `S3Presigner` beans are now produced in the same way as other clients.
 */
public class S3PresignerProcessor {

    S3PresignerBuildTimeConfig buildTimeConfig;

    protected DotName presignerClientName() {
        return DotName.createSimple(S3Presigner.class.getName());
    }

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        if (buildTimeConfig.unremovableS3PresignerBean()) {
            requireClientInjectionProducer
                    .produce(new RequireAmazonClientInjectionBuildItem(presignerClientName(), ClientUtil.DEFAULT_CLIENT_NAME));
        }
    }
}
