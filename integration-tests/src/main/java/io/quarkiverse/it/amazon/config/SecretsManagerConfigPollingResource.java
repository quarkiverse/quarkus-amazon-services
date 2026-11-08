package io.quarkiverse.it.amazon.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.ConfigProvider;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Path("/configuration/secret-polling")
public class SecretsManagerConfigPollingResource {
    @Inject
    SecretsManagerClient client;

    @GET
    @Produces("text/plain")
    public String value() {
        return ConfigProvider.getConfig().getValue("test.sm.polling", String.class);
    }

    @PUT
    public void update(String value) {
        client.putSecretValue(request -> request.secretId("polling-secret").secretString(value));
    }
}
