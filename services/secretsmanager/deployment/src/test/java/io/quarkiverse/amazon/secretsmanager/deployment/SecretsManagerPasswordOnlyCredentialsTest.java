package io.quarkiverse.amazon.secretsmanager.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

class SecretsManagerPasswordOnlyCredentialsTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.secretsmanager.devservices.provider", "floci")
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.devservices.username", "app_user")
            .overrideConfigKey("quarkus.datasource.devservices.password", "database-password")
            .overrideConfigKey("quarkus.datasource.jdbc.min-size", "0")
            .overrideConfigKey("quarkus.datasource.jdbc.initial-size", "0")
            .overrideRuntimeConfigKey("quarkus.datasource.username", "app_user")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider", "db")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider-name", "secretsmanager-credentials-provider")
            .overrideRuntimeConfigKey("quarkus.secretsmanager.credentials-provider.db.secrets.password", "database/password")
            .overrideRuntimeConfigKey("quarkus.secretsmanager.credentials-provider.db.batch-retrieval", "true");

    @Inject
    SecretsManagerClient secrets;

    @Inject
    AgroalDataSource datasource;

    @Test
    void localUsernameAndBatchedRawPasswordAuthenticate() throws Exception {
        secrets.createSecret(request -> request.name("database/password").secretString("database-password"));
        try (var connection = datasource.getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT current_user")) {
            result.next();
            assertEquals("app_user", result.getString(1));
        }
    }
}
