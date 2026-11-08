package io.quarkiverse.amazon.common.runtime;

import java.util.ArrayList;
import java.util.Collections;

import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

/** Configure bootstrap clients before CDI and the Quarkus executor are available. */
public final class AwsConfigSourceClient {
    private AwsConfigSourceClient() {
    }

    public static SmallRyeConfigBuilder configBuilder(ConfigSourceContext context) {
        var profiles = new ArrayList<>(context.getProfiles());
        Collections.reverse(profiles);
        return new SmallRyeConfigBuilder().withProfiles(profiles)
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withSources(context.getConfigSources()).withValidateUnknown(false);
    }

    public static <B extends AwsClientBuilder<B, C> & AwsSyncClientBuilder<B, C>, C> void configure(B builder, String service,
            AmazonClientConfig client,
            SyncHttpClientConfig transport, ConfigSourceContext context) {
        if (client.aws().credentials().filter(config -> config.type() == AwsCredentialsProviderType.CUSTOM).isPresent()) {
            throw new RuntimeConfigurationError("quarkus." + service
                    + ".aws.credentials.type=custom requires CDI, which is not available to config sources.");
        }
        try {
            AmazonClientCommonRecorder.initAwsClient(builder, service, service, client.aws(), client.aws());
            AmazonClientCommonRecorder.initSdkClientEndpoint(builder, service, service, client.sdk(), client.sdk());
            // Leave SDK timeout/retry scheduling to the SDK; the Quarkus executor has not started yet.
            builder.overrideConfiguration(overrides -> {
                client.sdk().apiCallTimeout().ifPresent(overrides::apiCallTimeout);
                client.sdk().apiCallAttemptTimeout().ifPresent(overrides::apiCallAttemptTimeout);
            });
            // Bootstrap always uses the bundled JDK transport, independently of CDI clients' transport selection.
            builder.httpClientBuilder(new AmazonClientUrlConnectionTransportRecorder()
                    .configureSync(service, new RuntimeValue<>(transport)).getValue());
        } catch (RuntimeException failure) {
            throw new RuntimeConfigurationError("Unable to configure quarkus." + service + " bootstrap client ["
                    + failure.getClass().getSimpleName() + "]; check AWS, API timeout and sync-client settings.");
        }
    }

}
