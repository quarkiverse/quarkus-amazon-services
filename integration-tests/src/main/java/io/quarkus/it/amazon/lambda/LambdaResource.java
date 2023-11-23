package io.quarkus.it.amazon.lambda;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
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

    private final AtomicBoolean lambdaCreated = new AtomicBoolean(false);

    @Inject
    LambdaUtils lambdaUtils;

    @POST
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlocking(byte[] function) {
        LOG.info("Testing Blocking Lambda client with lambda: " + lambdaUtils.getLambdaName());
        createLambda(function);
        return processFuncitonConfig(lambdaUtils.findDevLambdaBlocking());
    }

    @POST
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync(byte[] function) {
        LOG.info("Testing Async Lambda client with lambda: " + lambdaUtils.getLambdaName());

        createLambda(function);
        return lambdaUtils.findDevLambdaAsync()
                .thenApply(this::processFuncitonConfig);
    }

    private void createLambda(byte[] function) {
        if (lambdaCreated.get()) {
            return;
        }
        lambdaUtils.createLambda(function);
        lambdaCreated.set(true);
    }

    private String processFuncitonConfig(FunctionConfiguration lambda) {
        if (StringUtils.equals(lambdaUtils.getLambdaName(), lambda.functionName())) {
            return OK;
        }
        return ERROR;
    }

}
