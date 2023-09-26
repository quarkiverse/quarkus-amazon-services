package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClientBuilder;

public class SecretsManagerConfigSourceAsyncFactory
        implements ConfigSourceFactory.ConfigurableConfigSourceFactory<SecretsManagerConfigConfig> {

    private static final int ORDINAL = 270; // this is higher than the file system or jar ordinals, but lower than env vars

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, SecretsManagerConfigConfig config) {

        var builder = client("amazon-secretsmanager-config", "secretsmanager-config", config);

        var provider = new SecretsManagerConfigSourceAsyncProvider(builder.build(), config, ORDINAL);

        return provider.getConfigSources(SecretsManagerConfigSourceAsyncFactory.class.getClassLoader());
    }

    private SecretsManagerAsyncClientBuilder client(String clientName, String configName, SecretsManagerConfigConfig config) {

        AmazonClientCommonRecorder clientRecorder = new AmazonClientCommonRecorder();
        SecretsManagerConfigRecorder secretsManagerConfigRecorder = new SecretsManagerConfigRecorder(config);

        RuntimeValue<SdkAsyncHttpClient.Builder> transportBuilder = SecretsManagerBootstrapHolder.getTransportRecorder()
                .configureAsync(clientName,
                        new RuntimeValue<>(config.asyncClient()));
        ScheduledExecutorService executor = null;

        SecretsManagerConfigBuildTimeConfig buildTimeConfig = new SecretsManagerConfigBuildTimeConfig() {

            @Override
            public SdkBuildTimeConfig sdk() {
                return new SdkBuildTimeConfig() {

                    @Override
                    public Optional<List<String>> interceptors() {
                        return SecretsManagerBootstrapHolder.getInterceptors();
                    }
                };
            }

            @Override
            public SyncHttpClientBuildTimeConfig syncClient() {
                throw new UnsupportedOperationException("Unimplemented method 'syncClient'");
            }

            @Override
            public AsyncHttpClientBuildTimeConfig asyncClient() {
                throw new UnsupportedOperationException("Unimplemented method 'asyncClient'");
            }

            @Override
            public SecretsManagerDevServicesBuildTimeConfig devservices() {
                throw new UnsupportedOperationException("Unimplemented method 'devservices'");
            }
        };

        RuntimeValue<AwsClientBuilder> secretsManagerBootstrapClientBuilder = clientRecorder.configure(
                secretsManagerConfigRecorder.createAsyncBuilder(transportBuilder, executor),
                secretsManagerConfigRecorder.getAwsConfig(), secretsManagerConfigRecorder.getSdkConfig(),
                buildTimeConfig, executor, configName);

        return (SecretsManagerAsyncClientBuilder) secretsManagerBootstrapClientBuilder.getValue();
    }
}
