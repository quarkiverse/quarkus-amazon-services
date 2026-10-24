package io.quarkiverse.it.amazon.appconfigdata;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/appconfigdata")
public class AppConfigDataResource {

    @ConfigProperty(name = "application", defaultValue = "N/A")
    String applicationName;

    @GET
    @Path("config")
    @Produces(TEXT_PLAIN)
    public String testConfig() {
        return "application: " + applicationName;
    }
}
