package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.DockerClientFactory;

import io.quarkus.test.QuarkusUnitTest;

public class DevServicesWithComposeDevServicesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("compose-devservices.properties",
                            "application.properties"));

    @Test
    public void test() {
        var networkName = DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> container.getImage().contains("localstack")).toList()
                .get(0)
                .getNetworkSettings().getNetworks().keySet().stream().toList().get(0);
        assertTrue(
                networkName.startsWith("quarkus-devservices-quarkus-amazon-s3-deployment") && networkName.endsWith("_default"));
    }
}
