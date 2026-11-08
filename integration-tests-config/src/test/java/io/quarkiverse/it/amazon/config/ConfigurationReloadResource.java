package io.quarkiverse.it.amazon.config;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.credentials.CredentialsProvider;

@Path("/reload")
public class ConfigurationReloadResource {
    @Inject
    @Named("secretsmanager-credentials-provider")
    CredentialsProvider secrets;

    @Inject
    @Named("ssm-credentials-provider")
    CredentialsProvider parameters;

    @GET
    @Produces("text/plain")
    public String snapshot() {
        var config = ConfigProvider.getConfig();
        return Stream.of("ssm", "sm", "appconfig", "expression.sm", "expression.ssm", "reload.marker")
                .map(key -> config.getValue(key, String.class)).collect(Collectors.joining("|"))
                + "|" + secrets.getCredentials("reload").get("password")
                + "|" + parameters.getCredentials("reload").get("password") + "|source-0";
    }
}
