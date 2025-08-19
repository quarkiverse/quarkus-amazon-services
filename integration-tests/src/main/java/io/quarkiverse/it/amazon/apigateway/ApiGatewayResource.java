package io.quarkiverse.it.amazon.apigateway;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.GetRestApiResponse;

@Path("/apigateway")
public class ApiGatewayResource {

    private static final Logger LOG = Logger.getLogger(ApiGatewayResource.class);

    @Inject
    ApiGatewayClient apiGatewayClient;

    @Inject
    ApiGatewayAsyncClient apiGatewayAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync API Gateway client");
        // Create API Gateway
        var apiId = apiGatewayClient
                .createRestApi(r -> r.name("Test API"))
                .id();
        // Get API Gateway
        return apiGatewayClient
                .getRestApi(r -> r.restApiId(apiId))
                .name();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async API Gateway client");
        // Create API Gateway then get name
        return apiGatewayAsyncClient
                .createRestApi(r -> r.name("Test API Async"))
                .thenApply(CreateRestApiResponse::id)
                .thenCompose(apiId -> apiGatewayAsyncClient.getRestApi(r -> r.restApiId(apiId)))
                .thenApply(GetRestApiResponse::name);
    }
}
