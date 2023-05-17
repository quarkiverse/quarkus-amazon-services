package io.quarkus.amazon.devservices.cognitouserpools;

import java.net.*;

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
            final InetAddress fetchedIpAddress = InetAddress.getByName(address);

            if (fetchedIpAddress instanceof Inet6Address) {
                // RFC 2732 specifies that IPv6 addresses should be surrounded by brackets when used as a URI reference
                ipAddress = String.format("[%s]", fetchedIpAddress.getHostAddress());
            } else {
                ipAddress = fetchedIpAddress.getHostAddress();
            }

            return new URI("http://" + ipAddress + ":" + getMappedPort(PORT));
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }
}
