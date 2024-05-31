package io.quarkiverse.it.amazon.localstack;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/redshift")
public class RedshiftResource {

    private static final Logger LOG = Logger.getLogger(RedshiftResource.class);

    @ConfigProperty(name = "quarkus.redshift.endpoint-override")
    String endPointOverride;

    @GET
    @Path("test")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing additional service for localstack");

        return endPointOverride;
    }
}
