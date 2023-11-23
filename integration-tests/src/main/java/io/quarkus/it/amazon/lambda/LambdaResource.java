package io.quarkus.it.amazon.lambda;

import static io.quarkus.it.amazon.lambda.LambdaUtils.LAMBDA;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.utils.StringUtils;

/*
 * Created by triphon 21.11.23 г.
 */
@Path("/lambda")
public class LambdaResource {
    private static final Logger LOG = Logger.getLogger(LambdaResource.class);
    public static final String HELLO_WORLD = "Hello World!";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    @Inject
    LambdaUtils lambdaUtils;

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlocking() {
        LOG.info("Testing Blocking Lambda client with lambda: " + LAMBDA);
        return processFuncitonConfig(lambdaUtils.findDevLambdaBlocking());
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Blocking Lambda client with lambda: " + LAMBDA);

        return lambdaUtils.findDevLambdaAsync()
                .thenApply(LambdaResource::processFuncitonConfig);
    }

    private static String processFuncitonConfig(FunctionConfiguration lambda) {
        if (StringUtils.equals(LAMBDA, lambda.functionName())) {
            return OK;
        }
        return ERROR;
    }

}
