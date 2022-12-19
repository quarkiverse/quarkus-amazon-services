package io.quarkus.amazon.cognitouserpools.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClientBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClientBuilder;

@ApplicationScoped
public class CognitoUserPoolsClientProducer {
    private final CognitoIdentityProviderClient syncClient;
    private final CognitoIdentityProviderAsyncClient asyncClient;

    CognitoUserPoolsClientProducer(Instance<CognitoIdentityProviderClientBuilder> syncClientBuilderInstance,
            Instance<CognitoIdentityProviderAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public CognitoIdentityProviderClient client() {
        if (syncClient == null) {
            throw new IllegalStateException(
                    "The CognitoIdentityProviderClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public CognitoIdentityProviderAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException(
                    "The CognitoIdentityProviderAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
