package io.quarkus.amazon.common.runtime;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.CDI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@Recorder
public class AmazonClientCommonRecorder {
    private static final Log LOG = LogFactory.getLog(AmazonClientCommonRecorder.class);

    public RuntimeValue<AwsClientBuilder> configure(RuntimeValue<? extends AwsClientBuilder> clientBuilder,
            RuntimeValue<HasAmazonClientRuntimeConfig> amazonClientConfigRuntime, HasSdkBuildTimeConfig sdkBuildTimeConfig,
            ScheduledExecutorService scheduledExecutorService, String awsServiceName, String clientName) {
        AwsClientBuilder builder = clientBuilder.getValue();

        AmazonClientConfig config = amazonClientConfigRuntime.getValue().clients().get(clientName);

        initAwsClient(builder, awsServiceName, config.aws());
        initSdkClient(builder, awsServiceName, config.sdk(), sdkBuildTimeConfig.sdk(), scheduledExecutorService);

        return new RuntimeValue<>(builder);
    }

    public void initAwsClient(AwsClientBuilder builder, String extension, AwsConfig config) {
        config.region().ifPresent(builder::region);

        builder.credentialsProvider(config.credentials().type().create(config.credentials(), "quarkus." + extension));
    }

    public void initSdkClient(SdkClientBuilder builder, String extension, SdkConfig config, SdkBuildTimeConfig buildConfig,
            ScheduledExecutorService scheduledExecutorService) {
        if (config.endpointOverride().isPresent()) {
            URI endpointOverride = config.endpointOverride().get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        config.endpointOverride().filter(URI::isAbsolute).ifPresent(builder::endpointOverride);

        final ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();

        if (config.advanced().useQuarkusScheduledExecutorService()) {
            // use quarkus executor service
            overrides.scheduledExecutorService(scheduledExecutorService);
        }

        config.apiCallTimeout().ifPresent(overrides::apiCallTimeout);
        config.apiCallAttemptTimeout().ifPresent(overrides::apiCallAttemptTimeout);

        buildConfig.interceptors().orElse(Collections.emptyList()).stream()
                .map(String::trim)
                .map(this::createInterceptor)
                .filter(Objects::nonNull)
                .forEach(overrides::addExecutionInterceptor);
        builder.overrideConfiguration(overrides.build());
    }

    public RuntimeValue<SdkPresigner.Builder> configurePresigner(
            RuntimeValue<? extends SdkPresigner.Builder> clientBuilder,
            RuntimeValue<HasAmazonClientRuntimeConfig> amazonClientConfigRuntime,
            String awsServiceName, String clientName) {
        SdkPresigner.Builder builder = clientBuilder.getValue();

        AmazonClientConfig config = amazonClientConfigRuntime.getValue().clients().get(clientName);

        initAwsPresigner(builder, awsServiceName, config.aws());
        initSdkPresigner(builder, awsServiceName, config.sdk());

        return new RuntimeValue<>(builder);
    }

    public void initAwsPresigner(SdkPresigner.Builder builder, String extension, AwsConfig config) {
        config.region().ifPresent(builder::region);

        builder.credentialsProvider(config.credentials().type().create(config.credentials(), "quarkus." + extension));
    }

    public void initSdkPresigner(SdkPresigner.Builder builder, String extension, SdkConfig config) {
        if (config.endpointOverride().isPresent()) {
            URI endpointOverride = config.endpointOverride().get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        config.endpointOverride().filter(URI::isAbsolute).ifPresent(builder::endpointOverride);
    }

    private ExecutionInterceptor createInterceptor(String interceptorClassName) {
        try {
            Class<ExecutionInterceptor> classObj = (Class<ExecutionInterceptor>) Thread.currentThread().getContextClassLoader()
                    .loadClass(interceptorClassName);
            try {
                return CDI.current().select(classObj).get();
            } catch (UnsatisfiedResolutionException e) {
                // silent fail
            }
            return classObj.getConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            LOG.error("Unable to create interceptor " + interceptorClassName, e);
            return null;
        }
    }

    public Function<SyntheticCreationalContext<AwsClient>, AwsClient> build(Class<?> clazz, String clientName) {
        return new Function<SyntheticCreationalContext<AwsClient>, AwsClient>() {

            @Override
            public AwsClient apply(SyntheticCreationalContext<AwsClient> context) {
                SdkBuilder builder;
                if (ClientUtil.isDefaultClient(clientName))
                    builder = (SdkBuilder) context.getInjectedReference(clazz);
                else
                    builder = (SdkBuilder) context.getInjectedReference(clazz,
                            new io.quarkus.amazon.common.AmazonClientBuilder.AwsClientBuilderLiteral(clientName));

                return (AwsClient) builder.build();
            }
        };
    }

    public Function<SyntheticCreationalContext<SdkPresigner>, SdkPresigner> buildPresigner(Class<?> clazz, String clientName) {
        return new Function<SyntheticCreationalContext<SdkPresigner>, SdkPresigner>() {

            @Override
            public SdkPresigner apply(SyntheticCreationalContext<SdkPresigner> context) {
                SdkPresigner.Builder builder;
                if (ClientUtil.isDefaultClient(clientName))
                    builder = (SdkPresigner.Builder) context.getInjectedReference(clazz);
                else
                    builder = (SdkPresigner.Builder) context.getInjectedReference(clazz,
                            new io.quarkus.amazon.common.AmazonClientBuilder.AwsClientBuilderLiteral(clientName));

                return (SdkPresigner) builder.build();
            }
        };
    }
}
