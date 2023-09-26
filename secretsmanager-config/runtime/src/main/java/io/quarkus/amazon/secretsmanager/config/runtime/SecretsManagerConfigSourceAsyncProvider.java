package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest.Builder;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;

public class SecretsManagerConfigSourceAsyncProvider extends SecretsManagerConfigSourceProvider {

    SecretsManagerAsyncClient secretsManagerClient;

    public SecretsManagerConfigSourceAsyncProvider(SecretsManagerAsyncClient secretsManagerAsyncClient,
            SecretsManagerConfigConfig config,
            int ordinal) {
        super(config, ordinal);
        this.secretsManagerClient = secretsManagerAsyncClient;
    }

    @Override
    List<SecretListEntry> listSecretsPaginator(Consumer<Builder> listSecretsRequestBuilder) {
        List<SecretListEntry> result = new ArrayList<>();
        secretsManagerClient.listSecretsPaginator(listSecretsRequestBuilder).subscribe(c -> result.addAll(c.secretList()))
                .join();

        return result;
    }

    @Override
    GetSecretValueResponse getSecretValue(Consumer<GetSecretValueRequest.Builder> getSecretValueRequestBuilder) {
        return secretsManagerClient.getSecretValue(getSecretValueRequestBuilder).join();
    }
}
