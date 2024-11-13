package io.quarkus.it.amazon.acm;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.acm.AcmAsyncClient;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.model.CertificateDetail;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResponse;
import software.amazon.awssdk.services.acm.model.RequestCertificateResponse;

import java.util.concurrent.CompletionStage;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/acm")
public class AcmResource {

    private static final Logger LOG = Logger.getLogger(AcmResource.class);

    private static final String DOMAIN_NAME = "quarkus.local";

    @Inject
    AcmClient acmClient;

    @Inject
    AcmAsyncClient acmAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync ACM client");
        // Request certificate
        var arn = acmClient
                .requestCertificate(r -> r.domainName(DOMAIN_NAME))
                .certificateArn();
        // Get domain name
        return acmClient
                .describeCertificate(r -> r.certificateArn(arn))
                .certificate()
                .domainName();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async ACM client");
        // Request certificate then get domain name
        return acmAsyncClient
                .requestCertificate(r -> r.domainName(DOMAIN_NAME))
                .thenApply(RequestCertificateResponse::certificateArn)
                .thenCompose(arn -> acmAsyncClient.describeCertificate(r -> r.certificateArn(arn)))
                .thenApply(DescribeCertificateResponse::certificate)
                .thenApply(CertificateDetail::domainName);
    }
}
