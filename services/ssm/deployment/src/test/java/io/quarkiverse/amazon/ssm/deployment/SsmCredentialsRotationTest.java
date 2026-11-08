package io.quarkiverse.amazon.ssm.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;

class SsmCredentialsRotationTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.ssm.devservices.provider", "floci")
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.devservices.username", "app_user")
            .overrideConfigKey("quarkus.datasource.devservices.password", "initial-password")
            .overrideConfigKey("quarkus.datasource.jdbc.min-size", "0")
            .overrideConfigKey("quarkus.datasource.jdbc.initial-size", "0")
            .overrideConfigKey("quarkus.datasource.jdbc.max-size", "1")
            .overrideConfigKey("quarkus.datasource.jdbc.max-lifetime", "2S")
            .overrideRuntimeConfigKey("quarkus.datasource.username", "app_user")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider", "db")
            .overrideRuntimeConfigKey("quarkus.datasource.credentials-provider-name", "ssm-credentials-provider")
            .overrideRuntimeConfigKey("quarkus.ssm.credentials-provider.db.parameters.password", "/rotation/password")
            .overrideRuntimeConfigKey("quarkus.ssm.credentials-provider.db.batch-retrieval", "true");

    @Inject
    SsmClient parameters;

    @Inject
    AgroalDataSource datasource;

    @Test
    void recycledConnectionsUseTheUpdatedSecureParameterAndLocalUsername() throws Exception {
        parameters.putParameter(request -> request.name("/rotation/password").type(ParameterType.SECURE_STRING)
                .value("initial-password"));
        int initialPid;
        try (var connection = datasource.getConnection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT pg_backend_pid()")) {
                result.next();
                initialPid = result.getInt(1);
            }
            statement.execute("ALTER ROLE app_user PASSWORD 'rotated-password'");
            parameters.putParameter(request -> request.name("/rotation/password").type(ParameterType.SECURE_STRING)
                    .value("rotated-password").overwrite(true));
        }
        String url = ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class);
        var rejected = assertThrows(SQLException.class, () -> {
            try (var ignored = DriverManager.getConnection(url, "app_user", "initial-password")) {
                throw new AssertionError("Old database password still authenticates");
            }
        });
        assertEquals("28P01", rejected.getSQLState());
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            try (var connection = datasource.getConnection();
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT pg_backend_pid(), current_user")) {
                result.next();
                assertNotEquals(initialPid, result.getInt(1));
                assertEquals("app_user", result.getString(2));
            }
        });
    }
}
