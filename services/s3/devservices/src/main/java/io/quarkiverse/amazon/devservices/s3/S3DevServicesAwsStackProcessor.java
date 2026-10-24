package io.quarkiverse.amazon.devservices.s3;

import java.util.Map;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3BuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3DevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

public class S3DevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupS3(S3BuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("s3", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void overrideAwsStackClientConfig(AwsStackContainer awsStackContainer, Map<String, String> config) {
        config.put("quarkus.s3.path-style-access", "true");
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createBuckets(localstack, (S3DevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createBuckets(AwsStackContainer localstack, S3DevServicesBuildTimeConfig configuration) {

        if (configuration.buckets().isEmpty()) {
            return;
        }

        try (S3Client client = localstack.configure(S3Client.builder())
                .forcePathStyle(true)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            for (var bucketName : configuration.buckets()) {
                try {
                    client.createBucket(b -> b.bucket(bucketName));
                } catch (BucketAlreadyOwnedByYouException e) {
                    // the bucket already exists in a shared container
                }
            }
        }
    }
}
