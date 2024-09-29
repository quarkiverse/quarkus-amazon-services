package io.quarkiverse.it.amazon.iam;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.AmazonClient;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.CreateUserResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;

@Path("/iam")
public class IamResource {

    private static final Logger LOG = Logger.getLogger(IamResource.class);

    @Inject
    IamClient iamClient;

    @Inject
    IamAsyncClient iamAsyncClient;

    @Inject
    @AmazonClient("custom")
    IamClient iamClientNamedCustom;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync IAM client");
        CreateUserResponse user = iamClient.createUser(CreateUserRequest.builder().userName("quarkus-sync").build());

        return String.valueOf(user.sdkHttpResponse().statusCode());
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public String testAsync() throws InterruptedException, ExecutionException {
        LOG.info("Testing Async IAM client");

        CompletableFuture<CreateUserResponse> user = iamAsyncClient
                .createUser(CreateUserRequest.builder().userName("quarkus-async").build());

        return String.valueOf(user.get().sdkHttpResponse().statusCode());
    }

    @GET
    @Path("account")
    @Produces(TEXT_PLAIN)
    public String testCustomSync() throws InterruptedException, ExecutionException {
        LOG.info("Testing Named Sync IAM client");

        GetUserResponse user = iamClient.getUser();
        GetUserResponse userNamed = iamClientNamedCustom.getUser();

        return user.user().userId() + ":" + userNamed.user().userId();
    }
}
