package io.quarkiverse.it.amazon.sesv2;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.AlreadyExistsException;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest.Builder;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@Path("/sesv2")
public class SesV2Resource {

    private static final Logger LOG = Logger.getLogger(SesV2Resource.class);
    public final static String EMAIL_TXT_BODY = "Quarkus is awesome";
    public final static String EMAIL_SUBJECT = "Quarkus";
    public final static String SYNC_FROM = "sync-sender-v2@example.com";
    public final static String SYNC_TO = "sync-recipient-v2@example.com";
    public final static String ASYNC_FROM = "async-sender-v2@example.com";
    public final static String ASYNC_TO = "async-recipient-v2@example.com";

    @Inject
    SesV2Client sesClient;

    @Inject
    SesV2AsyncClient sesAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync SES V2 client");
        //Verify the sender address
        try {
            sesClient.createEmailIdentity(identity -> identity.emailIdentity(SYNC_FROM));
        } catch (AlreadyExistsException ex) {
            LOG.info("Email identity already exists: " + SYNC_FROM);
        }
        checkVerified(SYNC_FROM, sesClient.getEmailIdentity(identity -> identity.emailIdentity(SYNC_FROM)));
        //Send email
        return sesClient.sendEmail(emailRequest(SYNC_FROM, SYNC_TO)).messageId();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SES V2 client");

        return sesAsyncClient.createEmailIdentity(identity -> identity.emailIdentity(ASYNC_FROM))
                .exceptionally(ex -> {
                    if (unwrap(ex) instanceof AlreadyExistsException) {
                        LOG.info("Email identity already exists: " + ASYNC_FROM);
                        return null;
                    }
                    throw new CompletionException(ex);
                })
                .thenCompose(r -> sesAsyncClient.getEmailIdentity(identity -> identity.emailIdentity(ASYNC_FROM)))
                .thenAccept(r -> checkVerified(ASYNC_FROM, r))
                .thenCompose(r -> sesAsyncClient.sendEmail(emailRequest(ASYNC_FROM, ASYNC_TO)))
                .thenApply(SendEmailResponse::messageId);
    }

    private static void checkVerified(String address, GetEmailIdentityResponse identity) {
        if (!Boolean.TRUE.equals(identity.verifiedForSendingStatus())) {
            throw new IllegalStateException("Email identity is not verified for sending: " + address);
        }
    }

    private static Throwable unwrap(Throwable ex) {
        return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
    }

    private static Consumer<Builder> emailRequest(String from, String to) {
        return req -> req
                .fromEmailAddress(from)
                .destination(dest -> dest.toAddresses(to))
                .content(content -> content.simple(msg -> msg
                        .subject(s -> s.data(EMAIL_SUBJECT))
                        .body(body -> body.text(text -> text.data(EMAIL_TXT_BODY)))));
    }
}
