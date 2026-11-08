package io.quarkiverse.it.amazon.config;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.ConfigProvider;

@Path("/reload")
public class ConfigurationReloadResource {
    @GET
    @Produces("text/plain")
    public String snapshot() {
        var config = ConfigProvider.getConfig();
        return Stream.of("ssm", "sm", "appconfig", "expression.sm", "expression.ssm", "reload.marker")
                .map(key -> config.getValue(key, String.class)).collect(Collectors.joining("|"))
                + "|source-0";
    }
}
