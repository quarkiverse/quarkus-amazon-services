package io.quarkiverse.amazon.ecr.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Generated("io.quarkiverse.amazon:codegen")
@ApplicationScoped
public class EcrBean {
    @Inject
    EcrAsyncClient asyncClient;

    @Inject
    EcrClient syncClient;

    public EcrBean() {
    }

    public String invokeAsyncClient() {
        return this.asyncClient.serviceName();
    }

    public String invokeSyncClient() {
        return this.syncClient.serviceName();
    }
}