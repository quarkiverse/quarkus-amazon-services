package io.quarkiverse.amazon.s3.runtime;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.AmazonClientBuilder;
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

    public RuntimeValue<S3CrtAsyncClientBuilder> getCrtAsyncClientBuilder(String awsServiceName, String clientName) {
        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();
        configureS3Client(builder, awsServiceName, clientName);

        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3CrtAsyncClientBuilder builder, String awsServiceName, String clientName) {
        S3Config.S3ClientConfig defaults = config.getValue().s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME);
        S3Config.S3ClientConfig client = config.getValue().s3ConfigFor(clientName);
        builder
                .accelerate(client.accelerateMode().or(defaults::accelerateMode).orElse(false))
                .checksumValidationEnabled(client.checksumValidation().or(defaults::checksumValidation).orElse(true))
                .crossRegionAccessEnabled(client.useArnRegionEnabled().or(defaults::useArnRegionEnabled).orElse(false))
                .forcePathStyle(client.pathStyleAccess().or(defaults::pathStyleAccess).orElse(false));

        S3CrtConfig crt = client.crtClient();
        S3CrtConfig crtDefaults = defaults.crtClient();
        crt.initialReadBufferSizeInBytes().or(crtDefaults::initialReadBufferSizeInBytes)
                .ifPresent(builder::initialReadBufferSizeInBytes);
        crt.maxConcurrency().or(crtDefaults::maxConcurrency).ifPresent(builder::maxConcurrency);
        crt.minimumPartSizeInBytes().or(crtDefaults::minimumPartSizeInBytes).ifPresent(builder::minimumPartSizeInBytes);
        crt.targetThroughputInGbps().or(crtDefaults::targetThroughputInGbps).ifPresent(builder::targetThroughputInGbps);
        crt.maxNativeMemoryLimitInBytes().or(crtDefaults::maxNativeMemoryLimitInBytes)
                .ifPresent(builder::maxNativeMemoryLimitInBytes);

        AwsConfig awsDefaults = config.getValue().defaultsFor(clientName).aws();
        AwsConfig awsConfig = config.getValue().clients().get(clientName).aws();
        SdkConfig sdkConfig = config.getValue().clients().get(clientName).sdk();
        String prefix = config.getValue().configPrefix("s3", clientName);

        awsConfig.region().or(awsDefaults::region).ifPresent(builder::region);
        builder.credentialsProvider(
                AmazonClientCommonRecorder.resolveCredentialsProvider(awsServiceName, prefix, awsConfig, awsDefaults));
        AmazonClientCommonRecorder
                .resolveEndpointOverride(awsServiceName, prefix, sdkConfig, config.getValue().defaultsFor(clientName).sdk())
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

    public Function<SyntheticCreationalContext<S3AsyncClient>, S3AsyncClient> getS3CrtAsyncClient(String clientName) {
        return new Function<SyntheticCreationalContext<S3AsyncClient>, S3AsyncClient>() {
            @Override
            public S3AsyncClient apply(SyntheticCreationalContext<S3AsyncClient> context) {
                return context.getInjectedReference(S3CrtAsyncClientBuilder.class,
                        ClientUtil.isDefaultClient(clientName) ? new Annotation[0]
                                : new Annotation[] { new AmazonClientBuilder.AmazonClientBuilderLiteral(clientName) })
                        .build();
            }
        };
    }
}
