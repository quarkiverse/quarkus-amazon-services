package io.quarkiverse.amazon.devservices.s3;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3BuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3DevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3DevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    private static final String AWS_PATH_STYLE_ACCESS = "quarkus.s3.path-style-access";

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupS3(S3BuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(Service.S3, clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareLocalStack(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createBuckets(localstack, getConfiguration((S3DevServicesBuildTimeConfig) clientBuildTimeConfig));
    }

    @Override
    protected void overrideDefaultConfig(Map<String, String> defaultConfig) {
        // Forces this client to use path-style addressing for buckets. Localstack
        // returns an ip as host
        // and it confuse DefaultS3EndpointProvider ruleset
        defaultConfig.put(AWS_PATH_STYLE_ACCESS, "true");
    }

    public void createBuckets(AwsStackContainer localstack, S3DevServiceCfg configuration) {
        try (S3Client client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(localstack.getAccessKey(), localstack.getSecretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            for (var bucketName : configuration.buckets) {
                client.createBucket(b -> b.bucket(bucketName));
            }
        }
    }

    private S3DevServiceCfg getConfiguration(S3DevServicesBuildTimeConfig devServicesConfig) {
        return new S3DevServiceCfg(devServicesConfig);
    }

    private static final class S3DevServiceCfg extends LocalStackDevServicesBaseConfig {
        private final Set<String> buckets;

        public S3DevServiceCfg(S3DevServicesBuildTimeConfig config) {
            super(config.shared(), config.isolated(), config.serviceName(), config.containerProperties());
            this.buckets = config.buckets();
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && Objects.equals(buckets, ((S3DevServiceCfg) o).buckets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                    buckets);
        }
    }
}
