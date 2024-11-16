package io.quarkus.it.amazon.sqsmessagingconnector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ApplicationScoped
public class SqsMessagingConnectorManager {
    @Inject
    SqsClient sqsClient;
    List<String> messages = new CopyOnWriteArrayList<>();

    @Incoming("messages")
    public void process(String incomingMessage) {
        messages.add(incomingMessage);
    }

    public void sendMessage(String message, String queueName) {
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(getQueueUrl(queueName)).messageBody(message).build());
    }

    public List<String> getMessages() {
        return messages;
    }

    private String getQueueUrl(String queueName) {
        return sqsClient.listQueues(r -> r.queueNamePrefix(queueName)).queueUrls().get(0);
    }
}
