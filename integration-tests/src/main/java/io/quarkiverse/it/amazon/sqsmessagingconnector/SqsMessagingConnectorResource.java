package io.quarkiverse.it.amazon.sqsmessagingconnector;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/sqs-messaging-connector")
public class SqsMessagingConnectorResource {
    @Inject
    SqsMessagingConnectorManager connectorManager;

    @Path("messages/{queueName}")
    @POST
    public void sendSyncMessage(String queueName, @RestQuery String message) {
        connectorManager.sendMessage(message, queueName);
    }

    @Path("messages/{queueName}")
    @GET
    public List<String> getMessages() {
        return connectorManager.getMessages();
    }
}
