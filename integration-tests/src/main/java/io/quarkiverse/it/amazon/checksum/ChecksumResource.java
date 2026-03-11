package io.quarkiverse.it.amazon.checksum;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;

@Path("/checksum")
@RegisterForReflection(targets = { DefaultChecksumAlgorithm.class })
public class ChecksumResource {

    @ConfigProperty(name = "crt.ispresent", defaultValue = "false")
    Boolean crtIsPresent;

    @GET
    @Path("create")
    @Produces(TEXT_PLAIN)
    public String testCreate() {

        int i = 0;
        // get all fields of DefaultChecksumAlgorithm by reflection to enumerates all algorithms, and create checksum for each algorithm
        for (var field : DefaultChecksumAlgorithm.class.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(ChecksumAlgorithm.class)) {
                i++;
                try {
                    ChecksumAlgorithm algorithm = (ChecksumAlgorithm) field.get(null);
                    SdkChecksum.forAlgorithm(algorithm);

                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (RuntimeException e) {
                    if (!crtIsPresent
                            && e.getMessage().contains("Add dependency on 'software.amazon.awssdk.crt:aws-crt' module")) {
                        // if crt is not present, the crt implementation will throw exception (Add dependency on 'software.amazon.awssdk.crt:aws-crt' module) we can ignore it.
                    } else {
                        throw e;
                    }
                }
            }
        }

        return "ok:" + i;
    }
}
