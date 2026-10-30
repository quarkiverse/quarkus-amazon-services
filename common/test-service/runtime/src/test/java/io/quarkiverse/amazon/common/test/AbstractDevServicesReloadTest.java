package io.quarkiverse.amazon.common.test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.quarkus.test.QuarkusDevModeTest;

/** Verifies real reloads reuse a controlled HTTP container and close old SDK threads on both extension paths. */
public abstract class AbstractDevServicesReloadTest {
    protected abstract QuarkusDevModeTest application();

    protected abstract Class<? extends AbstractLifecycleResource> resourceClass();

    protected abstract String prefix();

    public static QuarkusDevModeTest configure(QuarkusDevModeTest app, String prefix, String awsSuffix,
            Class<? extends AbstractLifecycleResource> resource) {
        return app.withApplicationRoot(jar -> jar.addClasses(resource, AbstractLifecycleResource.class)
                .addClasses(ReloadTestStackProcessor.class, ReloadTestStackProcessor.Stack.class)
                .addAsResource(new StringAsset(ReloadTestStackProcessor.class.getName()), "META-INF/quarkus-build-steps.list")
                .addAsResource(new StringAsset("""
                        %s%s.region=us-east-1
                        %s.api-call-timeout=10s
                        quarkus.aws.devservices.shared=false
                        quarkus.aws.devservices.provider=floci
                        quarkus.aws.devservices.floci.enabled=false
                        %s.async-client.event-loop.number-of-threads=2
                        %s.async-client.event-loop.thread-name-prefix=amazon-client-reload
                        """.formatted(prefix, awsSuffix, prefix, prefix, prefix)), "application.properties"));
    }

    @Test
    public void repeatedReloadsClosePreviousSdkThreadsAndKeepTheStack() {
        given().get("/lifecycle/request").then().statusCode(200).body(is("ok"));
        String endpoint = given().get("/lifecycle/endpoints").then().statusCode(200).extract().asString();
        String containerId = given().get("/lifecycle/container-id").then().statusCode(200).extract().asString();
        var docker = DockerClientFactory.instance().client();
        String startedAt = docker.inspectContainerCmd(containerId).exec().getState().getStartedAt();
        for (int seconds = 11; seconds <= 13; seconds++) {
            Set<Thread> previous = sdkThreads();
            assertFalse(previous.isEmpty(), "A real async request must start SDK threads");
            String timeout = seconds + "s";
            String previousTimeout = (seconds - 1) + "s";
            application().modifyResourceFile("application.properties", value -> value.replace(
                    prefix() + ".api-call-timeout=" + previousTimeout,
                    prefix() + ".api-call-timeout=" + timeout));
            String previousName = seconds == 11 ? "archive" : "archive" + (seconds - 1);
            String nextName = "archive" + seconds;
            application().modifySourceFile(resourceClass(), source -> source.replace(
                    "@AmazonClient(\"" + previousName + "\")", "@AmazonClient(\"" + nextName + "\")"));
            given().get("/lifecycle/timeout").then().statusCode(200).body(is("PT" + seconds + "S"));
            given().get("/lifecycle/request").then().statusCode(200).body(is("ok"));
            given().get("/lifecycle/endpoints").then().statusCode(200).body(is(endpoint));
            given().get("/lifecycle/container-id").then().statusCode(200).body(is(containerId));
            var state = docker.inspectContainerCmd(containerId).exec().getState();
            assertTrue(state.getRunning());
            assertEquals(startedAt, state.getStartedAt());
            await().atMost(Duration.ofSeconds(20)).until(() -> previous.stream().noneMatch(Thread::isAlive));
        }
    }

    private static Set<Thread> sdkThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("amazon-client-reload") || thread.getName().startsWith("sdk-"))
                .collect(Collectors.toSet());
    }
}
