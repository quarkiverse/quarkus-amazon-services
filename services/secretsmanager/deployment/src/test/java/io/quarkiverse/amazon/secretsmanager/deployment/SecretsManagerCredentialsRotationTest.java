package io.quarkiverse.amazon.secretsmanager.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/** Real PostgreSQL authentication and Agroal recycling, with Secrets Manager emulated by Floci. */
class SecretsManagerCredentialsRotationTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.secretsmanager.devservices.provider", "floci")
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.devservices.username", "app_user")
            .overrideConfigKey("quarkus.datasource.devservices.password", "initial-password")
            .overrideConfigKey("quarkus.datasource.devservices.db-name", "rotation")
            .overrideConfigKey("quarkus.datasource.jdbc.min-size", "0")
            .overrideConfigKey("quarkus.datasource.jdbc.initial-size", "0")
            .overrideConfigKey("quarkus.datasource.jdbc.max-size", "1")
            .overrideConfigKey("quarkus.datasource.jdbc.max-lifetime", "2S")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider", "db")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider-name", "secretsmanager-credentials-provider")
            .overrideRuntimeConfigKey("quarkus.secretsmanager.credentials-provider.db.secret-id", "rotation/database");

    @Inject
    SecretsManagerClient secrets;

    @Inject
    AgroalDataSource datasource;

    @Test
    void aRecycledConnectionAuthenticatesWithTheRotatedSecret() throws Exception {
        secrets.createSecret(request -> request.name("rotation/database")
                .secretString("{\"username\":\"app_user\",\"password\":\"initial-password\"}"));
        int initialPid;
        try (var connection = datasource.getConnection()) {
            initialPid = pid(connection);
            try (var statement = connection.createStatement()) {
                statement.execute("ALTER ROLE app_user PASSWORD 'rotated-password'");
            }
            secrets.putSecretValue(request -> request.secretId("rotation/database")
                    .secretString("{\"username\":\"app_user\",\"password\":\"rotated-password\"}"));
        }
        String jdbcUrl = ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class);
        var rejected = assertThrows(SQLException.class, () -> {
            try (var ignored = DriverManager.getConnection(jdbcUrl, "app_user", "initial-password")) {
                throw new AssertionError("Old database password still authenticates");
            }
        });
        assertEquals("28P01", rejected.getSQLState());
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            try (var connection = datasource.getConnection()) {
                assertNotEquals(initialPid, pid(connection));
                try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT current_user")) {
                    result.next();
                    assertEquals("app_user", result.getString(1));
                }
            }
        });
    }

    private static int pid(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT pg_backend_pid()")) {
            result.next();
            return result.getInt(1);
        }
    }
}
