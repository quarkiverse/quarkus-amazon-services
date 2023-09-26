package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;

public class SecretsManagerConfigSourceSyncProvider extends SecretsManagerConfigSourceProvider {

    SecretsManagerClient secretsManagerClient;

    public SecretsManagerConfigSourceSyncProvider(SecretsManagerClient syncClientBuilder,
            SecretsManagerConfigConfig config,
            int ordinal) {
        super(config, ordinal);
        this.secretsManagerClient = syncClientBuilder;
    }

    @Override
    List<SecretListEntry> listSecretsPaginator(Consumer<ListSecretsRequest.Builder> listSecretsRequestBuilder) {
        return secretsManagerClient.listSecretsPaginator(listSecretsRequestBuilder).stream()
                .flatMap(response -> response.secretList().stream())
                .collect(Collectors.toList());
    }

    @Override
    GetSecretValueResponse getSecretValue(Consumer<GetSecretValueRequest.Builder> getSecretValueRequestBuilder) {
        return secretsManagerClient.getSecretValue(getSecretValueRequestBuilder);
    }
}
