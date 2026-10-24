package io.quarkiverse.it.amazon.sns;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkiverse.it.amazon.sqs.SqsConsumerManager;
import io.quarkiverse.it.amazon.sqs.SqsQueueManager;
import software.amazon.awssdk.services.sns.SnsClient;

@Path("/sns")
public class SnsResource {
    @Inject
    SnsManager snsManager;

    @Inject
    SqsQueueManager queueManager;

    @Inject
    SqsConsumerManager queueConsumer;

    @Inject
    SnsClient snsClient;

    private final List<String> httpNotifications = new CopyOnWriteArrayList<>();

    /**
     * Dev Services creates the configured topics again, with the same attributes, when it reuses a shared container.
     */
    @Path("devservices-topic-replay")
    @GET
    @Produces(TEXT_PLAIN)
    public String testDevServicesTopicReplay() {
        snsClient.createTopic(t -> t.name("quarkus-test-topic.fifo").attributes(Map.of("FifoTopic", "true")));
        return "OK";
    }

    @Path("topics/{topicName}")
    @POST
    public void createTopicAndSubscribeQueue(String topicName) {
        snsManager.createTopic(topicName);
        snsManager.subscribe(topicName, queueManager.createQueue(topicName));
    }

    @Path("topics/{topicName}")
    @DELETE
    public void deleteQueue(String topicName) {
        snsManager.deleteTopic(topicName);
        queueManager.deleteQueue(topicName);
    }

    @Path("topics/{topicName}")
    @GET
    @Produces(TEXT_PLAIN)
    public String readPublishedMessages(String topicName) {
        return queueConsumer.receiveSync(topicName).stream().collect(Collectors.joining(" "));
    }

    @Path("topics/{topicName}/http-subscriptions")
    @POST
    @Produces(TEXT_PLAIN)
    public String subscribeHttp(String topicName, @RestQuery String endpoint) {
        return snsManager.subscribeHttp(topicName, endpoint);
    }

    /**
     * Called back by the stack container, e.g. with the SubscriptionConfirmation of an HTTP subscription.
     */
    @Path("http-notifications")
    @POST
    public void receiveHttpNotification(String body) {
        httpNotifications.add(body);
    }

    @Path("http-notifications")
    @GET
    @Produces(TEXT_PLAIN)
    public String readHttpNotifications() {
        return String.join(" ", httpNotifications);
    }

    @Path("sync/publish/{topicName}")
    @POST
    @Produces(TEXT_PLAIN)
    public String publishSync(String topicName, @RestQuery String message) {
        return snsManager.publishSync(topicName, message);
    }

    @Path("async/publish/{topicName}")
    @POST
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> publishAsync(String topicName, @RestQuery String message) {
        return snsManager.publishAsync(topicName, message);
    }
}
