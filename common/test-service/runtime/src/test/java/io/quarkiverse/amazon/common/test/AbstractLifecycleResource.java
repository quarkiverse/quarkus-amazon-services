package io.quarkiverse.amazon.common.test;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.awscore.AwsClient;

/** Real SDK operations shared by historical and generic production/reload fixtures. */
public abstract class AbstractLifecycleResource {
    @ConfigProperty(name = "test.aws.container-id", defaultValue = "none")
    String containerId;

    protected abstract AwsClient sync();

    protected abstract AwsClient async();

    protected abstract CompletionStage<?> request();

    @GET
    @Path("/request")
    public CompletionStage<String> call() {
        return request().thenApply(ignored -> "ok");
    }

    @GET
    @Path("/container-id")
    public String containerId() {
        return containerId;
    }

    @GET
    @Path("/endpoints")
    public String endpoints() {
        return sync().serviceClientConfiguration().endpointOverride().map(Object::toString).orElse("aws")
                + "|" + async().serviceClientConfiguration().endpointOverride().map(Object::toString).orElse("aws");
    }

    @GET
    @Path("/timeout")
    public String timeout() {
        return sync().serviceClientConfiguration().overrideConfiguration().apiCallTimeout().orElseThrow().toString();
    }
}
