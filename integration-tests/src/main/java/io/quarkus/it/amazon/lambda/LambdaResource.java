package io.quarkus.it.amazon.lambda;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Path("/lambda")
public class LambdaResource {
    private static final Logger LOG = Logger.getLogger(LambdaResource.class);

    @Inject
    LambdaClient lambdaClient;

    @Inject
    LambdaAsyncClient lambdaAsyncClient;

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Blocking Lambda client");
        return lambdaClient.listFunctions().functions().get(0).functionName();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async Lambda client");
        return lambdaAsyncClient.listFunctions()
                .thenApply(listFunctionsResponse -> listFunctionsResponse.functions().get(0).functionName());
    }
}
