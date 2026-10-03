package io.quarkiverse.it.amazon.ssm;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterType;

@Path("/ssm")
public class SsmResource {

    private static final Logger LOG = Logger.getLogger(SsmResource.class);
    public final static String TEXT = "Quarkus is awesome";
    private static final String SYNC_PARAM = "/quarkus/sync-" + UUID.randomUUID().toString();
    private static final String ASYNC_PARAM = "/quarkus/async-" + UUID.randomUUID().toString();

    @Inject
    SsmClient ssmClient;

    @Inject
    SsmAsyncClient ssmAsyncClient;

    @ConfigProperty(name = "pgsql.user", defaultValue = "N/A")
    String postgresUsername;

    @ConfigProperty(name = "pgsql.password", defaultValue = "N/A")
    String postgresPassword;

    @ConfigProperty(name = "pgsql.jdbc", defaultValue = "N/A")
    String postgresUrl;

    // Injected values from JSON secrets (flattened) and fetched recursively
    @ConfigProperty(name = "app-db-config.db1.host", defaultValue = "N/A")
    String db1Host;

    @ConfigProperty(name = "app-db-config.db1.port", defaultValue = "N/A")
    String db1Port;

    @ConfigProperty(name = "app-db-config.db2.host", defaultValue = "N/A")
    String db2Host;

    @ConfigProperty(name = "app-db-config.db2.port", defaultValue = "N/A")
    String db2Port;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync SSM client with parameter: " + SYNC_PARAM);
        //Put parameter
        ssmClient.putParameter(r -> r.name(SYNC_PARAM).type(ParameterType.SECURE_STRING).value(TEXT));
        //Get parameter
        return ssmClient.getParameter(r -> r.name(SYNC_PARAM).withDecryption(Boolean.TRUE)).parameter().value();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SSM client with parameter: " + ASYNC_PARAM);
        //Put and get parameter
        return ssmAsyncClient.putParameter(r -> r.name(ASYNC_PARAM).type(ParameterType.SECURE_STRING).value(TEXT))
                .thenCompose(result -> ssmAsyncClient.getParameter(r -> r.name(ASYNC_PARAM).withDecryption(Boolean.TRUE)))
                .thenApply(GetParameterResponse::parameter)
                .thenApply(Parameter::value);
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
        return "db1.host: " + db1Host + ", db1.port: " + db1Port + ", db2.host: " + db2Host + ", db2.port: " + db2Port;
    }
}
