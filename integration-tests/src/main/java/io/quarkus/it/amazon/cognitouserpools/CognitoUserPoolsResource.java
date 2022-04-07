package io.quarkus.it.amazon.cognitouserpools;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;

@Path("/cognito-user-pools")
public class CognitoUserPoolsResource {
    @Inject
    CognitoIdentityProviderClient cognitoIdpClient;

    @Inject
    CognitoIdentityProviderAsyncClient cognitoIdpAsyncClient;

    @GET
    @Path("/sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        final ListUserPoolsRequest listRequest = ListUserPoolsRequest.builder().build();
        cognitoIdpClient.listUserPools(listRequest);
        return "sync-success";
    }

    @GET
    @Path("/async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        final ListUserPoolsRequest listRequest = ListUserPoolsRequest.builder().build();
        return cognitoIdpAsyncClient
                .listUserPools(listRequest)
                .thenApply(response -> "async-success");
    }
}
