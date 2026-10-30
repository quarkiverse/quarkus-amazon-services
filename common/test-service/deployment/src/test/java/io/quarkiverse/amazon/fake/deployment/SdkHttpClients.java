package io.quarkiverse.amazon.fake.deployment;

import java.util.List;
import java.util.stream.Stream;

import io.quarkus.maven.dependency.Dependency;
import software.amazon.awssdk.core.util.VersionInfo;

/**
 * Selects HTTP dependencies for extension fixtures. Missing-dependency tests also exclude the transports
 * declared at test scope for the real dev-mode reload fixture.
 */
public final class SdkHttpClients {

    public static final String URL_CONNECTION = "url-connection-client";
    public static final String APACHE = "apache-client";
    public static final String APACHE5 = "apache5-client";
    public static final String NETTY = "netty-nio-client";
    public static final String AWS_CRT = "aws-crt-client";

    private SdkHttpClients() {
    }

    /**
     * @return the dependencies to force in a {@code QuarkusExtensionTest} to add the given HTTP clients
     */
    public static List<Dependency> of(String... artifactIds) {
        return Stream.of(artifactIds)
                .map(artifactId -> Dependency.of("software.amazon.awssdk", artifactId, VersionInfo.SDK_VERSION))
                .toList();
    }
}
