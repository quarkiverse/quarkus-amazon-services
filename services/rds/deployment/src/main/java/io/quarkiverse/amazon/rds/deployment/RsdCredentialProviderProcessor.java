package io.quarkiverse.amazon.rds.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.rds.runtime.RdsCredentialsProvider;
import io.quarkiverse.amazon.rds.runtime.RdsCredentialsProviderBuildTimeConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.services.rds.RdsClient;

public class RsdCredentialProviderProcessor {
    public static final DotName RDS_CLIENT = DotName.createSimple(RdsClient.class);

    RdsCredentialsProviderBuildTimeConfig buildTimeConfig;

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(RdsCredentialsProvider.class)
                .build();
    }

    @BuildStep
    void setup(BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {
        for (var entry : buildTimeConfig.credentialsProvider().entrySet()) {
            var config = entry.getValue();
            if (config.useQuarkusClient()) {
                requireClientInjectionProducer.produce(new RequireAmazonClientInjectionBuildItem(
                        RDS_CLIENT, config.name().orElse(ClientUtil.DEFAULT_CLIENT_NAME)));
            }
        }
    }
}
