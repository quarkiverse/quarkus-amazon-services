package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.utils.StringUtils;

@Recorder
public class AmazonClientCommonRecorder {
    private static final Log LOG = LogFactory.getLog(AmazonClientCommonRecorder.class);

    public RuntimeValue<AwsClientBuilder> configure(RuntimeValue<? extends AwsClientBuilder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig, HasSdkBuildTimeConfig sdkBuildTimeConfig,
            ScheduledExecutorService scheduledExecutorService, String awsServiceName) {
        AwsClientBuilder builder = clientBuilder.getValue();

        initAwsClient(builder, awsServiceName, awsConfig.getValue());
        initSdkClient(builder, awsServiceName, sdkConfig.getValue(), sdkBuildTimeConfig.sdk(), scheduledExecutorService);

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
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig,
            String awsServiceName) {
        SdkPresigner.Builder builder = clientBuilder.getValue();

        initAwsPresigner(builder, awsServiceName, awsConfig.getValue());
        initSdkPresigner(builder, awsServiceName, sdkConfig.getValue());

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
            return (ExecutionInterceptor) Class
                    .forName(interceptorClassName, false, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create interceptor " + interceptorClassName, e);
            return null;
        }
    }
}
