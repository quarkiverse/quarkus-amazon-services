package io.quarkus.it.amazon.localstack;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
