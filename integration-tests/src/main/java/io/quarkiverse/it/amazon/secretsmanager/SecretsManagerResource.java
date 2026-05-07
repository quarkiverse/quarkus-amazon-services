package io.quarkiverse.it.amazon.secretsmanager;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Path("/secretsmanager")
public class SecretsManagerResource {

    private static final Logger LOG = Logger.getLogger(SecretsManagerResource.class);
    public final static String TEXT = "Quarkus is awesome";
    private static final String SYNC_PARAM = "quarkus/sync-" + UUID.randomUUID().toString();
    private static final String ASYNC_PARAM = "quarkus/async-" + UUID.randomUUID().toString();

    @Inject
    SecretsManagerClient secretsManagerClient;

    @Inject
    SecretsManagerAsyncClient secretsManagerAsyncClient;

    @ConfigProperty(name = "postgres.username", defaultValue = "N/A")
    String postgresUsername;

    @ConfigProperty(name = "postgres.password", defaultValue = "N/A")
    String postgresPassword;

    @ConfigProperty(name = "postgres.url", defaultValue = "N/A")
    String postgresUrl;

    // Injected values from JSON secrets (flattened)
    @ConfigProperty(name = "db.host", defaultValue = "N/A")
    String dbHost;

    @ConfigProperty(name = "db.port", defaultValue = "N/A")
    String dbPort;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync Secrets Manager client with secret name: " + SYNC_PARAM);
        //Put parameter
        secretsManagerClient.createSecret(r -> r.name(SYNC_PARAM).secretString(TEXT));
        //Get parameter
        return secretsManagerClient.getSecretValue(r -> r.secretId(SYNC_PARAM)).secretString();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SSM client with parameter: " + ASYNC_PARAM);
        //Put and get parameter
        return secretsManagerAsyncClient.createSecret(r -> r.name(ASYNC_PARAM).secretString(TEXT))
                .thenCompose(result -> secretsManagerAsyncClient.getSecretValue(r -> r.secretId(ASYNC_PARAM)))
                .thenApply(GetSecretValueResponse::secretString);
    }

    @GET
    @Path("config")
    @Produces(TEXT_PLAIN)
    public String testConfig() {
        return "postgresUsername: " + postgresUsername + ", postgresPassword: " + postgresPassword + ", postgresUrl: "
                + postgresUrl;
    }

    @GET
    @Path("config-json")
    @Produces(TEXT_PLAIN)
    public String testConfigJson() {
        LOG.info("Testing Secrets Manager JSON config parsing");
        return "db.host: " + dbHost + ", db.port: " + dbPort;
    }
}
