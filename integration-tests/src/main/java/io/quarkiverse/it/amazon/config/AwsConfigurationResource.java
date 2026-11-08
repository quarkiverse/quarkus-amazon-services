package io.quarkiverse.it.amazon.config;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.quarkiverse.amazon.common.config.AwsConfigSourceCloser;
import io.quarkus.arc.Arc;

/** Test-only application endpoints; all credential values are synthetic fixtures. */
@Path("/configuration")
public class AwsConfigurationResource {
    @GET
    @Path("closers")
    @Produces(TEXT_PLAIN)
    public String closers() {
        return Integer.toString(Arc.container().beanManager().getBeans(AwsConfigSourceCloser.class).size());
    }

}
