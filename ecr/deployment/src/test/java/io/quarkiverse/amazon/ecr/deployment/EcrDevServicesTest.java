package io.quarkiverse.amazon.ecr.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.ecr.EcrClient;

public class EcrDevServicesTest {

    @Inject
    Instance<EcrClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.aws.devservices.localstack.image-name=localstack/localstack:3.0.1"),
                            "application.properties"));

    @Test
    public void test() {
        assertNotNull(client.get());
    }
}
