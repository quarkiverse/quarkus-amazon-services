package io.quarkiverse.amazon.ssm.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public class SsmConfigSourceFactory implements ConfigSourceFactory.ConfigurableConfigSourceFactory<SsmConfigConfig> {

    private static final Logger LOG = Logger.getLogger(SsmConfigSourceFactory.class);

    private static final int ORDINAL = 50;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext, SsmConfigConfig ssmConfig) {

        if (!ssmConfig.enabled()) {
            return Collections.emptyList();
        }

        Optional<String> pathOpt = ssmConfig.path().filter(p -> !isBlank(p));
        List<String> names = sanitizeNames(ssmConfig.names().orElse(List.of()));

        if (pathOpt.isEmpty() && names.isEmpty()) {
            LOG.error(
                    "SSM ConfigSource is enabled but neither quarkus.ssm.config.path nor quarkus.ssm.config.names is set.");
            return Collections.emptyList();
        }

        SsmConfig clientConfig = getClientConfig(configSourceContext);
        SsmClient client = build(clientConfig);
        return Collections.singletonList(new SsmConfigSource(client, pathOpt.orElse(null), names, ssmConfig.recursive(),
                ssmConfig.withDecryption(), ssmConfig.updateIntervalMinutes(), ORDINAL));
    }

    private static List<String> sanitizeNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (n != null && !isBlank(n)) {
                out.add(n.trim());
            }
        }
        return List.copyOf(out);
    }

    SsmClient build(SsmConfig config) {
        SsmClientBuilder builder = SsmClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder());
        AmazonClientConfig defaultConfig = config.clients().get(ClientUtil.DEFAULT_CLIENT_NAME);
        AmazonClientCommonRecorder.initAwsClient(builder, "ssm", "ssm", defaultConfig.aws(), defaultConfig.aws());
        AmazonClientCommonRecorder.initSdkClientEndpoint(builder, "ssm", "ssm", defaultConfig.sdk(), defaultConfig.sdk());
        return builder.build();
    }

    SsmConfig getClientConfig(ConfigSourceContext context) {
        List<String> profiles = new ArrayList<>(context.getProfiles());
        Collections.reverse(profiles);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withProfiles(profiles)
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withSources(context.getConfigSources())
                .withMapping(SsmConfig.class)
                .withValidateUnknown(false)
                .build();
        return config.getConfigMapping(SsmConfig.class);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(ORDINAL);
    }
}
