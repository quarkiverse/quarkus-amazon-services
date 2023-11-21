package io.quarkus.it.amazon.lambda;

import static io.quarkus.it.amazon.lambda.LambdaUtils.LAMBDA;
import static io.quarkus.it.amazon.lambda.LambdaUtils.getPayloadBuilder;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

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
    LambdaClient lambdaClient;

    @Inject
    LambdaAsyncClient lambdaAsyncClient;

    @Inject
    LambdaUtils lambdaUtils;

    FunctionConfiguration functionInfo;

    @PostConstruct
    public void init() {
        LOG.info("LambdaResource init");
        functionInfo = lambdaUtils.findDevLambda();
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlocking() {
        LOG.info("Testing Blocking Lambda client with lambda: " + LAMBDA);

        try {
            final InvokeResponse response = lambdaUtils.invokeLambda(functionInfo);
            final String responseAsString = response.payload().asUtf8String();
            if (!responseAsString.contains(HELLO_WORLD)) {
                throw new RuntimeException("Unexpected response: " + responseAsString);

            }
            return OK;
        } catch (Exception ex) {
            LOG.error("Error during Lambda operations.", ex);
            return ERROR;
        }
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncS3() {
        LOG.info("Testing Blocking Lambda client with lambda: " + LAMBDA);

        return lambdaAsyncClient.invoke(builder -> LambdaUtils.getPayloadBuilder(functionInfo, builder))
                .thenApply(response -> {
                    if (response.payload().asUtf8String().contains(HELLO_WORLD))
                        return OK;
                    return ERROR;
                })
                .exceptionally(th -> {
                    LOG.error("Error during async Lambda operations", th.getCause());
                    return ERROR;
                });
    }

}
