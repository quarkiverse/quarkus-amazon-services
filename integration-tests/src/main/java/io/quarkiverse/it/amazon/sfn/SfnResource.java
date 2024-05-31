package io.quarkiverse.it.amazon.sfn;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.sfn.SfnAsyncClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineRequest;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineResponse;

@Path("/sfn")
public class SfnResource {

    private static final Logger LOG = Logger.getLogger(SfnResource.class);

    @Inject
    SfnClient sfnClient;

    @Inject
    SfnAsyncClient sfnAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync SFN client");
        CreateStateMachineResponse stateMachine = sfnClient.createStateMachine(
                stateMachineRequest("sync-state-machine"));

        return stateMachine.stateMachineArn();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public String testAsync() throws InterruptedException, ExecutionException {
        LOG.info("Testing Async SFN client");

        CompletableFuture<CreateStateMachineResponse> stateMachine = sfnAsyncClient
                .createStateMachine(stateMachineRequest("async-state-machine"));

        return stateMachine.get().stateMachineArn();
    }

    private Consumer<CreateStateMachineRequest.Builder> stateMachineRequest(String name) {
        return builder -> builder
                .name(name)
                .definition(stateMachineDefinition())
                .build();
    }

    private String stateMachineDefinition() {
        return "{\n" +
                "  \"Comment\": \"A Hello World example of the Amazon States Language using Pass states\",\n" +
                "  \"StartAt\": \"HelloWorld\",\n" +
                "  \"States\": {\n" +
                "    \"HelloWorld\": {\n" +
                "      \"Type\": \"Pass\",\n" +
                "      \"Result\": \"Hello World!\",\n" +
                "      \"End\": true\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}
