package io.quarkiverse.it.amazon.sqs;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestQuery;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Path("/sqs")
public class SqsResource {

    @Inject
    SqsQueueManager queueMngr;

    @Inject
    SqsProducerManager producer;

    @Inject
    SqsConsumerManager consumer;

    @Inject
    SqsClient sqsClient;

    /**
     * Dev Services creates the configured queues again, with the same attributes, when it reuses a shared container.
     */
    @Path("devservices-queue-replay")
    @GET
    @Produces(TEXT_PLAIN)
    public String testDevServicesQueueReplay() {
        sqsClient.createQueue(q -> q.queueName("quarkus-messaging-test-queue"));
        sqsClient.createQueue(q -> q.queueName("quarkus-test-queue.fifo")
                .attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true")));
        return "OK";
    }

    @Path("queue/{queueName}")
    @POST
    public void createQueue(String queueName) {
        queueMngr.createQueue(queueName);
    }

    @Path("queue/{queueName}")
    @DELETE
    public void deleteQueue(String queueName) {
        queueMngr.deleteQueue(queueName);
    }

    @Path("sync/{queueName}")
    @POST
    @Produces(TEXT_PLAIN)
    public String sendSyncMessage(String queueName, @RestQuery String message) {
        return producer.sendSync(queueName, message);
    }

    @Path("sync/{queueName}")
    @GET
    @Produces(TEXT_PLAIN)
    public String getSyncMessages(String queueName) {
        return consumer.receiveSync(queueName).stream().collect(Collectors.joining(" "));
    }

    @Path("async/{queueName}")
    @POST
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> sendAsyncMessage(String queueName,
            @RestQuery String message) {
        return producer.sendAsync(queueName, message);
    }

    @Path("async/{queueName}")
    @GET
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> getAsyncMessages(String queueName) {
        return consumer.receiveAsync(queueName).thenApply(l -> l.stream().collect(Collectors.joining(" ")));
    }
}
