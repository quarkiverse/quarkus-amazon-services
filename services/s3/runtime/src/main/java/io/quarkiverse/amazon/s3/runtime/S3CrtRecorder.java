package io.quarkiverse.amazon.s3.runtime;

import java.util.concurrent.Executor;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncClientCompletion;
import io.quarkiverse.amazon.common.runtime.AwsConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.SdkConfig;
import io.quarkiverse.amazon.common.runtime.VertxContextCapturingInterceptor;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

@Recorder
public class S3CrtRecorder {
    private static final Logger LOG = Logger.getLogger(S3CrtRecorder.class);

    final RuntimeValue<S3Config> config;

    public S3CrtRecorder(RuntimeValue<S3Config> config) {
        this.config = config;
    }

    public RuntimeValue<S3CrtAsyncClientBuilder> getCrtAsyncClientBuilder(String awsServiceName) {
        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();
        configureS3Client(builder, awsServiceName);

        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3CrtAsyncClientBuilder builder, String awsServiceName) {
        builder
                .accelerate(config.getValue().accelerateMode())
                .checksumValidationEnabled(config.getValue().checksumValidation())
                .crossRegionAccessEnabled(config.getValue().useArnRegionEnabled())
                .forcePathStyle(config.getValue().pathStyleAccess());

        config.getValue().crtClient().initialReadBufferSizeInBytes().ifPresent(builder::initialReadBufferSizeInBytes);
        config.getValue().crtClient().maxConcurrency().ifPresent(builder::maxConcurrency);
        config.getValue().crtClient().minimumPartSizeInBytes().ifPresent(builder::minimumPartSizeInBytes);
        config.getValue().crtClient().targetThroughputInGbps().ifPresent(builder::targetThroughputInGbps);
        config.getValue().crtClient().maxNativeMemoryLimitInBytes().ifPresent(builder::maxNativeMemoryLimitInBytes);

        AwsConfig awsConfig = config.getValue().clients().get(ClientUtil.DEFAULT_CLIENT_NAME).aws();
        SdkConfig sdkConfig = config.getValue().clients().get(ClientUtil.DEFAULT_CLIENT_NAME).sdk();

        awsConfig.region().ifPresent(builder::region);
        builder.credentialsProvider(
                AmazonClientCommonRecorder.resolveCredentialsProvider(awsServiceName, awsServiceName, awsConfig, awsConfig));
        AmazonClientCommonRecorder.resolveEndpointOverride(awsServiceName, awsServiceName, sdkConfig, sdkConfig)
                .ifPresent(builder::endpointOverride);
    }

    public RuntimeValue<S3CrtAsyncClientBuilder> setExecutor(RuntimeValue<S3CrtAsyncClientBuilder> builder,
            Executor executor) {
        S3CrtAsyncClientBuilder sdkBuilder = builder.getValue();
        sdkBuilder.futureCompletionExecutor(
                AsyncClientCompletion.futureCompletionExecutor(config.getValue().asyncClient(), executor));
        return new RuntimeValue<>(sdkBuilder);
    }

    /**
     * Only recorded when the Vert.x extension is present, so that native applications without it do not reference its
     * classes.
     */
    public void propagateVertxContext(RuntimeValue<S3CrtAsyncClientBuilder> builder) {
        if (!config.getValue().asyncClient().advanced().vertxContextPropagation()) {
            return;
        }
        if (builder.getValue() instanceof S3CrtContextAwareBuilder contextAwareBuilder) {
            contextAwareBuilder.addContextInterceptor(new VertxContextCapturingInterceptor(true));
        } else {
            // The build-time transformation was skipped, see S3CrtContextPropagationProcessor
            LOG.warn("Vert.x context propagation is not supported by the S3 CRT client of this AWS SDK version");
        }
    }

    public Function<SyntheticCreationalContext<S3AsyncClient>, S3AsyncClient> getS3CrtAsyncClient() {
        return new Function<SyntheticCreationalContext<S3AsyncClient>, S3AsyncClient>() {
            @Override
            public S3AsyncClient apply(SyntheticCreationalContext<S3AsyncClient> context) {
                return context.getInjectedReference(S3CrtAsyncClientBuilder.class).build();
            }
        };
    }
}
