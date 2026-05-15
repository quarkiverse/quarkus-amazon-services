package io.quarkiverse.amazon.ecr.runtime;

import jakarta.enterprise.inject.Instance;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Generated("io.quarkiverse.amazon:codegen")
public class EcrSyntheticBean {
    private Instance<EcrAsyncClient> asyncInstance;

    private Instance<EcrClient> syncInstance;

    public EcrSyntheticBean(Instance<EcrAsyncClient> asyncInstance, Instance<EcrClient> syncInstance) {
        this.asyncInstance = asyncInstance;
        this.syncInstance = syncInstance;
    }

    public String invokeAsyncClient() {
        return this.asyncInstance.get().serviceName();
    }

    public String invokeSyncClient() {
        return this.syncInstance.get().serviceName();
    }
}