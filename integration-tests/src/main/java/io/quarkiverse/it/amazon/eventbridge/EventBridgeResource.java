package io.quarkiverse.it.amazon.eventbridge;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Path("/eventbridge")
public class EventBridgeResource {

    private static final Logger LOG = Logger.getLogger(EventBridgeResource.class);
    public final static String RULE_NAME = "quarkus-rule-";

    @Inject
    EventBridgeClient eventbridgeClient;

    @Inject
    EventBridgeAsyncClient eventbridgeAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync EventBridge client");

        eventbridgeClient.putRule(builder -> builder.name(RULE_NAME + "sync").scheduleExpression("rate(2 minutes)"));
        return eventbridgeClient.describeRule(builder -> builder.name(RULE_NAME + "sync")).arn();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async EventBridge client");

        return eventbridgeAsyncClient
                .putRule(builder -> builder.name(RULE_NAME + "async").scheduleExpression("rate(2 minutes)"))
                .thenCompose(discard -> eventbridgeAsyncClient.describeRule(builder -> builder.name(RULE_NAME + "async"))
                        .thenApply(r -> r.arn()));
    }
}
