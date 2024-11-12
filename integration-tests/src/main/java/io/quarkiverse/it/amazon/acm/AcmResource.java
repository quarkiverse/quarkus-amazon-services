package io.quarkiverse.it.amazon.acm;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.acm.AcmAsyncClient;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.model.CertificateDetail;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResponse;
import software.amazon.awssdk.services.acm.model.ImportCertificateResponse;

@Path("/acm")
public class AcmResource {

    private static final Logger LOG = Logger.getLogger(AcmResource.class);

    private static final SdkBytes CERTIFICATE = SdkBytes.fromUtf8String("""
            -----BEGIN CERTIFICATE-----
            MIIB1DCCAXmgAwIBAgIVAJ4BCcAB3Zrfhcv03ZqauYb2daL7MAoGCCqGSM49BAMCMDUxEDAOBgNV
            BAMMB1N1YjIgQ0ExFDASBgNVBAoMC1F1YXJraXZlcnNlMQswCQYDVQQGEwJVUzAeFw0yNDExMDgy
            MzMwNTFaFw0yNTAyMDgyMzMwNTFaMDsxCzAJBgNVBAYTAlVTMRQwEgYDVQQKDAtRdWFya2l2ZXJz
            ZTEWMBQGA1UEAwwNcXVhcmt1cy5sb2NhbDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABGMRoiHR
            aK/1sd4bJPnjTs1gjUbJct/3XIDPT737imjVkdnnfcve6r11k67j2c7DiYXxF9KIxsEHda2YdFrB
            e8qjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgWgMB0GA1UdDgQWBBQqMoBpbK9rf9Ek
            76tZ9+Xbxz55yzAfBgNVHSMEGDAWgBShaN3eufhKUHU1DZdyZGGTYm82NjAKBggqhkjOPQQDAgNJ
            ADBGAiEAqnQxdXcx6EMkzuYKpQ+73ZTZ6uyJoEBbx8ks+YqtB7oCIQCncKHDwBiMWpjoc2Ro8v3/
            XErQqWADBMmNGW0PHrWT8A==
            -----END CERTIFICATE-----
            """);

    private static final SdkBytes CERTIFICATE_CHAIN = SdkBytes.fromUtf8String("""
            -----BEGIN CERTIFICATE-----
            MIIBzzCCAXWgAwIBAgIUNud5F3uBx6oujHa8NO/78t1yWCQwCgYIKoZIzj0EAwIwNTEQMA4GA1UE
            AwwHU3ViMSBDQTEUMBIGA1UECgwLUXVhcmtpdmVyc2UxCzAJBgNVBAYTAlVTMB4XDTI0MTEwODIz
            MzA1MVoXDTM0MTEwODIzMzA1MVowNTEQMA4GA1UEAwwHU3ViMiBDQTEUMBIGA1UECgwLUXVhcmtp
            dmVyc2UxCzAJBgNVBAYTAlVTMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEs/u8POQjEfeWy66e
            GtSdLnpYh1mW3hinb7WZrirZ2n/WcxkUlXCTKlfjTU5H54U5v5my9Rv+0B+sd5OIlvXxoaNjMGEw
            DwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAYYwHQYDVR0OBBYEFKFo3d65+EpQdTUNl3Jk
            YZNibzY2MB8GA1UdIwQYMBaAFK8fZSvjLIDfIQeXKKkRAIxxyn6rMAoGCCqGSM49BAMCA0gAMEUC
            IQCz4n5nhQr2hjq1++tfI+kjkUJCWhUrDbPo1kqH3wl4iwIgWuYp7QcXfdlNdq5wXfFQxAiEOHdS
            2zqqWUqKPO3dJLc=
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIIB0TCCAXagAwIBAgIVANw9+aWvWliaF5Ewe17nf9OQtILZMAoGCCqGSM49BAMCMDUxEDAOBgNV
            BAMMB1Jvb3QgQ0ExFDASBgNVBAoMC1F1YXJraXZlcnNlMQswCQYDVQQGEwJVUzAeFw0yNDExMDgy
            MzMwNTBaFw00NDExMDgyMzMwNTBaMDUxEDAOBgNVBAMMB1N1YjEgQ0ExFDASBgNVBAoMC1F1YXJr
            aXZlcnNlMQswCQYDVQQGEwJVUzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABO3/cBy4jKoZNNLb
            hwkAMZmVnymhbSBjeykbeTWJOhVesiG/5stzRQkNoBE40USCmNmDtSHPkDpGuABbSeb9ugSjYzBh
            MA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMB0GA1UdDgQWBBSvH2Ur4yyA3yEHlyip
            EQCMccp+qzAfBgNVHSMEGDAWgBQkGPuBipJ3v2x3xW0gYR75aE051zAKBggqhkjOPQQDAgNJADBG
            AiEAqQAFjolQzMi+98W2TjqKfbqzRQ9OJOnzhxrlwrR0+dkCIQCn0NMa1G1EsYLTbEYZX8p/EWEA
            rXChXCbBQglhbfjMEA==
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIIBsDCCAVagAwIBAgIUM9MT9sXZgqxsTsxVCdv/L172UBkwCgYIKoZIzj0EAwIwNTEQMA4GA1UE
            AwwHUm9vdCBDQTEUMBIGA1UECgwLUXVhcmtpdmVyc2UxCzAJBgNVBAYTAlVTMCAXDTI0MTEwODIz
            MzA0OVoYDzIwNjQxMTA4MjMzMDQ5WjA1MRAwDgYDVQQDDAdSb290IENBMRQwEgYDVQQKDAtRdWFy
            a2l2ZXJzZTELMAkGA1UEBhMCVVMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATxe12l+aMlorfh
            gInjebEkAWLaekHd53gX3VaqkBcQuoje6JVfnveDDAh7nUa5PQHqTuPsX/VrTgL9tyY6YVdko0Iw
            QDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAdBgNVHQ4EFgQUJBj7gYqSd79sd8Vt
            IGEe+WhNOdcwCgYIKoZIzj0EAwIDSAAwRQIgWNCIdpPN8ixy//bYCRNq0wtQU7A+KZreFeZicPYR
            ctgCIQDYqxLA8r2FWUhVZs+IcY2/qjUi6YZKcE2xPMdIHCoOKQ==
            -----END CERTIFICATE-----
            """);

    private static final SdkBytes PRIVATE_KEY = SdkBytes.fromUtf8String("""
            -----BEGIN EC PARAMETERS-----
            BggqhkjOPQMBBw==
            -----END EC PARAMETERS-----
            -----BEGIN EC PRIVATE KEY-----
            MHcCAQEEIJLWR1RfO2Hz8JmRQozr1qMdcKSnjK+qCufDgtbzq9xuoAoGCCqGSM49
            AwEHoUQDQgAEYxGiIdFor/Wx3hsk+eNOzWCNRsly3/dcgM9PvfuKaNWR2ed9y97q
            vXWTruPZzsOJhfEX0ojGwQd1rZh0WsF7yg==
            -----END EC PRIVATE KEY-----
            """);

    @Inject
    AcmClient acmClient;

    @Inject
    AcmAsyncClient acmAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync ACM client");
        // Install certificate
        var arn = acmClient
                .importCertificate(r -> r
                        .certificate(CERTIFICATE)
                        .certificateChain(CERTIFICATE_CHAIN)
                        .privateKey(PRIVATE_KEY))
                .certificateArn();
        // Get domain name
        return acmClient
                .describeCertificate(r -> r
                        .certificateArn(arn))
                .certificate()
                .domainName();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async ACM client");
        // Install certificate and get domain name
        return acmAsyncClient
                .importCertificate(r -> r
                        .certificate(CERTIFICATE)
                        .certificateChain(CERTIFICATE_CHAIN)
                        .privateKey(PRIVATE_KEY))
                .thenApply(ImportCertificateResponse::certificateArn)
                .thenCompose(arn -> acmAsyncClient
                        .describeCertificate(r -> r
                                .certificateArn(arn)))
                .thenApply(DescribeCertificateResponse::certificate)
                .thenApply(CertificateDetail::domainName);
    }
}
