package io.quarkiverse.it.amazon.config;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.quarkiverse.amazon.common.config.AwsConfigSourceCloser;
import io.quarkus.arc.Arc;
import io.quarkus.credentials.CredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;

/** Test-only application endpoints; all credential values are synthetic fixtures. */
@Path("/configuration")
public class AwsConfigurationResource {
    @Inject
    @Named("secretsmanager-credentials-provider")
    CredentialsProvider secrets;

    @Inject
    @Named("ssm-credentials-provider")
    CredentialsProvider parameters;

    @Inject
    SecretsManagerClient secretsClient;

    @Inject
    SsmClient ssmClient;

    @GET
    @Path("closers")
    @Produces(TEXT_PLAIN)
    public String closers() {
        return Integer.toString(Arc.container().beanManager().getBeans(AwsConfigSourceCloser.class).size());
    }

    @GET
    @Path("credentials/{service}/{selection}")
    public Map<String, String> credentials(@PathParam("service") String service,
            @PathParam("selection") String selection) {
        return (service.equals("sm") ? secrets : parameters).getCredentials("qualification-" + selection);
    }

    @PUT
    @Path("credentials/{service}")
    public void rotate(@PathParam("service") String service, String password) {
        Map.of("document", "{\"username\":\"fixture-user\",\"password\":\"" + password + "\"}",
                "user", "fixture-user", "password", password).forEach((key, value) -> {
                    String id = "/credential-qualification/" + key;
                    if (service.equals("sm")) {
                        try {
                            secretsClient.createSecret(request -> request.name(id).secretString(value));
                        } catch (ResourceExistsException exists) {
                            secretsClient.putSecretValue(request -> request.secretId(id).secretString(value));
                        }
                    } else {
                        ssmClient.putParameter(request -> request.name(id).value(value)
                                .type(ParameterType.SECURE_STRING).overwrite(true));
                    }
                });
    }
}
