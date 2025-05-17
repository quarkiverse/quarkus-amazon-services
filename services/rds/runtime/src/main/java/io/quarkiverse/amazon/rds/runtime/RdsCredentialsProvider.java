package io.quarkiverse.amazon.rds.runtime;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.rds.runtime.RdsCredentialsProviderBuildTimeConfig.CredentialsProviderBuildTimeConfig;
import io.quarkiverse.amazon.rds.runtime.RdsCredentialsProviderConfig.CredentialsProviderConfig;
import io.quarkus.credentials.CredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;

@ApplicationScoped
@Named("rds-credentials-provider")
public class RdsCredentialsProvider implements CredentialsProvider {

    @Inject
    RdsCredentialsProviderConfig rdsConfig;

    @Inject
    RdsCredentialsProviderBuildTimeConfig rdsBuildTimeConfig;

    @Inject
    Instance<RdsClient> rdsClients;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {

        CredentialsProviderConfig credentialProviderConfig = rdsConfig.credentialsProvider().get(credentialsProviderName);
        if (credentialProviderConfig == null) {
            throw new IllegalArgumentException("unknown credentials provider with name " + credentialsProviderName);
        }

        var rdsClient = getRdsClient(rdsBuildTimeConfig.credentialsProvider().get(credentialsProviderName));
        var token = generateRdsAuthToken(credentialProviderConfig, rdsClient);

        return Map.ofEntries(
                Map.entry(USER_PROPERTY_NAME, credentialProviderConfig.username()),
                Map.entry(PASSWORD_PROPERTY_NAME, token));
    }

    private String generateRdsAuthToken(CredentialsProviderConfig config, RdsClient rdsClient) {

        return rdsClient.utilities().generateAuthenticationToken(builder -> builder
                .username(config.username())
                .port(config.port())
                .hostname(config.hostname()));
    }

    private RdsClient getRdsClient(CredentialsProviderBuildTimeConfig credentialsProviderBuildTimeConfig) {
        if (credentialsProviderBuildTimeConfig != null && credentialsProviderBuildTimeConfig.useQuarkusClient()) {
            return getRdsClient(credentialsProviderBuildTimeConfig.name().orElse(ClientUtil.DEFAULT_CLIENT_NAME));
        } else {
            return RdsClient.create();
        }
    }

    private RdsClient getRdsClient(String clientName) {
        return ClientUtil.isDefaultClient(clientName)
                ? rdsClients.select(Default.Literal.INSTANCE).get()
                : rdsClients.select(new AmazonClient.AmazonClientLiteral(clientName)).get();
    }
}
