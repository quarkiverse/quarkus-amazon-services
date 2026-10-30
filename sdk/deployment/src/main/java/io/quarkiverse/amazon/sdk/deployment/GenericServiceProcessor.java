package io.quarkiverse.amazon.sdk.deployment;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkBuildTimeConfig;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

final class GenericServiceProcessor extends AbstractAmazonServiceProcessor {
    private final AmazonSdkServiceBuildItem service;
    private final AmazonSdkBuildTimeConfig config;
    private final Class<?> syncBuilder;
    private final Class<?> asyncBuilder;

    GenericServiceProcessor(AmazonSdkServiceBuildItem service, AmazonSdkBuildTimeConfig config) {
        this.service = service;
        this.config = config;
        // The shared extension SPI still requires Class<?> for CDI builder types.
        this.syncBuilder = loadBuilder(service.syncBuilder);
        this.asyncBuilder = loadBuilder(service.asyncBuilder);
    }

    private static Class<?> loadBuilder(DotName builder) {
        try {
            return Class.forName(builder.toString(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load AWS SDK builder " + builder, e);
        }
    }

    void setup(AmazonSdkRecorder recorder, BuildProducer<AmazonClientExtensionBuildItem> extensions) {
        setupExtension(new RecorderAdapter(recorder), extensions);
    }

    void builders(AmazonSdkRecorder recorder, List<RequireAmazonClientInjectionBuildItem> injections,
            RecorderContext context, BuildProducer<GeneratedClassBuildItem> classes,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builders) {
        createExtensionBuilders(new RecorderAdapter(recorder) {
            private RuntimeValue<Supplier<AwsSyncClientBuilder<?, ?>>> syncFactory;
            private RuntimeValue<Supplier<AwsAsyncClientBuilder<?, ?>>> asyncFactory;

            @Override
            public RuntimeValue<AwsSyncClientBuilder<?, ?>> getSyncBuilder() {
                if (syncFactory == null) {
                    syncFactory = context.newInstance(ClientBuilderFactoryGenerator.generate(service.syncClient,
                            service.syncBuilder, classes));
                }
                return recorder.syncBuilder(syncFactory);
            }

            @Override
            public RuntimeValue<AwsAsyncClientBuilder<?, ?>> getAsyncBuilder() {
                if (asyncFactory == null) {
                    asyncFactory = context.newInstance(ClientBuilderFactoryGenerator.generate(service.asyncClient,
                            service.asyncBuilder, classes));
                }
                return recorder.asyncBuilder(asyncFactory);
            }
        }, injections, builders);
    }

    @Override
    protected String amazonServiceClientName() {
        return "amazon-sdk-" + service.service;
    }

    @Override
    protected String configName() {
        return "aws.services." + service.service;
    }

    @Override
    protected DotName syncClientName() {
        return service.syncClient;
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return syncBuilder;
    }

    @Override
    protected DotName asyncClientName() {
        return service.asyncClient;
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return asyncBuilder;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/" + service.service + "/execution.interceptors";
    }

    @Override
    protected HasTransportBuildTimeConfig transportBuildTimeConfig() {
        return config;
    }

    @Override
    protected HasSdkBuildTimeConfig sdkBuildTimeConfig() {
        return config;
    }

    private static class RecorderAdapter extends AmazonClientRecorder {
        // Build-time adapter only: every runtime value is produced by the recorded generic recorder.
        private final AmazonSdkRecorder recorder;

        RecorderAdapter(AmazonSdkRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public RuntimeValue<? extends HasAmazonClientRuntimeConfig> getAmazonClientsConfig() {
            return recorder.configuration();
        }

        @Override
        public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
            return recorder.syncConfig();
        }

        @Override
        public RuntimeValue<AsyncHttpClientConfig> getAsyncConfig() {
            return recorder.asyncConfig();
        }

        @Override
        public SyncHttpClientConfig getSyncClientConfig() {
            throw new UnsupportedOperationException("Use recorded runtime values");
        }

        @Override
        public AsyncHttpClientConfig getAsyncClientConfig() {
            throw new UnsupportedOperationException("Use recorded runtime values");
        }

        @Override
        public AwsSyncClientBuilder<?, ?> getSyncClientBuilder() {
            throw new UnsupportedOperationException("Use recorded runtime values");
        }

        @Override
        public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
            throw new UnsupportedOperationException("Use recorded runtime values");
        }
    }
}
