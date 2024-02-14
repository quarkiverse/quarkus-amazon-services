package io.quarkus.amazon.s3.runtime;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.quarkus.amazon.common.runtime.RuntimeConfigurationError;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

@Recorder
public class S3CrtRecorder {

    final S3Config config;

    public S3CrtRecorder(S3Config config) {
        this.config = config;
    }

    public RuntimeValue<S3CrtAsyncClientBuilder> getCrtAsyncClientBuilder(String awsServiceName) {
        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();
        configureS3Client(builder, awsServiceName);

        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3CrtAsyncClientBuilder builder, String awsServiceName) {
        builder
                .accelerate(config.accelerateMode())
                .checksumValidationEnabled(config.checksumValidation())
                .crossRegionAccessEnabled(config.useArnRegionEnabled())
                .forcePathStyle(config.pathStyleAccess());

        config.crtClient().initialReadBufferSizeInBytes().ifPresent(builder::initialReadBufferSizeInBytes);
        config.crtClient().maxConcurrency().ifPresent(builder::maxConcurrency);
        config.crtClient().minimumPartSizeInBytes().ifPresent(builder::minimumPartSizeInBytes);
        config.crtClient().targetThroughputInGbps().ifPresent(builder::targetThroughputInGbps);

        config.aws().region().ifPresent(builder::region);
        builder.credentialsProvider(
                config.aws().credentials().type().create(config.aws().credentials(), "quarkus." + awsServiceName));

        if (config.sdk().endpointOverride().isPresent()) {
            URI endpointOverride = config.sdk().endpointOverride().get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                awsServiceName,
                                endpointOverride.toString()));
            }
        }

        config.sdk().endpointOverride().filter(URI::isAbsolute).ifPresent(builder::endpointOverride);
    }

    public RuntimeValue<S3CrtAsyncClientBuilder> setExecutor(RuntimeValue<S3CrtAsyncClientBuilder> builder,
            LaunchMode launchMode, Executor executor) {
        if (launchMode == LaunchMode.NORMAL) {
            return new RuntimeValue<>(builder.getValue().futureCompletionExecutor(executor));
        } else {
            return new RuntimeValue<>(builder.getValue().futureCompletionExecutor(new S3CrtExecutorWrapper(executor)));
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

    /**
     * Capture the current ClassLoader and restore it to support dev and test mode
     */
    private static final class S3CrtExecutorWrapper implements Executor {

        private Executor executor;
        private ClassLoader contextClassLoader;

        private S3CrtExecutorWrapper(Executor executor) {
            this.executor = executor;
            this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void execute(Runnable command) {
            final ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                executor.execute(command);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}
