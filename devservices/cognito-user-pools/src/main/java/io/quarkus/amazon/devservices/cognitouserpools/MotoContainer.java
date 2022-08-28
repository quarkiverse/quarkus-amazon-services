package io.quarkus.amazon.devservices.cognitouserpools;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MotoContainer extends GenericContainer<MotoContainer> {

    static final int PORT = 5000;
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("motoserver/moto");

    /**
     * @param dockerImageName image name to use for Localstack
     */
    public MotoContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        waitingFor(Wait.forLogMessage("^ \\* Running on.*\\n", 1));
        withExposedPorts(PORT);
    }

    public URI getEndpointOverride() {
        try {
            final String address = getHost();
            String ipAddress = address;
            // resolve IP address and use that as the endpoint so that path-style access is automatically used by client
            ipAddress = InetAddress.getByName(address).getHostAddress();
            return new URI("http://" + ipAddress + ":" + getMappedPort(PORT));
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }
}
