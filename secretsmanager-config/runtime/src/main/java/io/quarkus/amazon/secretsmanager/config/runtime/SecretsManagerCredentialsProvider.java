package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkus.credentials.CredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@ApplicationScoped
@Named("secretsmanager-credentials-provider")
public class SecretsManagerCredentialsProvider implements CredentialsProvider {

    @Inject
    SecretsManagerClient client;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {

        return Map.of();
    }
}
