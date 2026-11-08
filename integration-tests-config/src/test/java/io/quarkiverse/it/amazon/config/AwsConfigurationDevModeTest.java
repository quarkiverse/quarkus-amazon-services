package io.quarkiverse.it.amazon.config;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.json.Json;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.QuarkusDevModeTest;

/** Real Quarkus restarts and SDK HTTP calls, with deterministic AWS protocol responses. */
public class AwsConfigurationDevModeTest {
    private static final AtomicInteger generation = new AtomicInteger();
    private static final ConcurrentLinkedQueue<String> unexpectedRequests = new ConcurrentLinkedQueue<>();
    private static final HttpServer server = startServer();

    @RegisterExtension
    static final QuarkusDevModeTest app = new QuarkusDevModeTest().withApplicationRoot(jar -> {
        StringBuilder properties = new StringBuilder("""
                quarkus.devservices.enabled=false
                quarkus.otel.enabled=false
                reload.marker=0
                quarkus.s3.config.enabled=true
                quarkus.s3.path-style-access=true
                quarkus.s3.config.update-interval=1H
                quarkus.s3.config.entries.app.bucket=bucket
                quarkus.s3.config.entries.app.key=config.properties
                quarkus.ssm.config.enabled=true
                quarkus.ssm.config.names[0]=/ssm
                quarkus.ssm.config.format=raw
                quarkus.ssm.config.update-interval=1H
                quarkus.secretsmanager.config.enabled=true
                quarkus.secretsmanager.config.update-interval=1H
                quarkus.secretsmanager.config.secrets.sm=sm
                quarkus.secretsmanager.config.format=raw
                quarkus.appconfigdata.config.enabled=true
                quarkus.appconfigdata.config.application=application
                quarkus.appconfigdata.config.environment=environment
                quarkus.appconfigdata.config.configuration-profile=profile
                quarkus.appconfigdata.config.update-interval=1H
                expression.sm=${sm::sm}
                expression.ssm=${ssm::/ssm}
                quarkus.secretsmanager.expression.cache-ttl=PT1H
                quarkus.ssm.expression.cache-ttl=PT1H
                quarkus.secretsmanager.credentials-provider.reload.secret-id=sm
                quarkus.secretsmanager.credentials-provider.reload.format=raw
                quarkus.ssm.credentials-provider.reload.parameter-name=/ssm
                quarkus.ssm.credentials-provider.reload.format=raw
                """);
        for (String service : new String[] { "s3", "ssm", "secretsmanager", "appconfigdata" }) {
            properties.append("""
                    quarkus.%s.endpoint-override=http://localhost:%d
                    quarkus.%s.aws.region=us-east-1
                    quarkus.%s.aws.credentials.type=anonymous
                    quarkus.%s.sync-client.type=url
                    quarkus.%s.api-call-timeout=PT5S
                    """.formatted(service, server.getAddress().getPort(), service, service, service, service));
        }
        jar.addClass(ConfigurationReloadResource.class)
                .addAsResource(new StringAsset(properties.toString()), "application.properties");
    });

    @Test
    void resourceAndSourceRestartsReplaceSnapshotsCachesAndPollers() {
        given().get("/reload").then().statusCode(200).body(is(snapshot(0, 0, 0, 0)));
        for (int next = 1; next <= 3; next++) {
            Set<Thread> previous = pollers();
            assertEquals(4, previous.size(), "Each AWS source must have exactly one poller");
            generation.set(next);
            int previousMarker = next == 3 ? 1 : next - 1;
            int previousSource = next == 3 ? 2 : 0;
            // Long-lived expression caches/snapshots stay old, whereas credential reads are always fresh.
            given().get("/reload").then().statusCode(200)
                    .body(is(snapshot(next - 1, previousMarker, next, previousSource)));
            if (next == 2) {
                app.modifySourceFile(ConfigurationReloadResource.class, source -> source.replace("source-0", "source-2"));
            } else {
                int marker = next;
                app.modifyResourceFile("application.properties", value -> value.replace(
                        "reload.marker=" + previousMarker, "reload.marker=" + marker));
            }
            int marker = next == 2 ? 1 : next;
            int source = next >= 2 ? 2 : 0;
            given().get("/reload").then().statusCode(200).body(is(snapshot(next, marker, next, source)));
            await().atMost(Duration.ofSeconds(10)).until(() -> previous.stream().noneMatch(Thread::isAlive));
            assertEquals(4, pollers().size(), "Reload must not accumulate polling threads");
        }
        assertTrue(unexpectedRequests.isEmpty(), unexpectedRequests::toString);
    }

    private static String snapshot(int value, int marker, int credential, int source) {
        return (value + "|").repeat(6) + marker + "|" + credential + "|" + credential + "|source-" + source;
    }

    private static Set<Thread> pollers() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("quarkus-amazon-") && thread.getName().endsWith("-config-poll"))
                .collect(Collectors.toSet());
    }

    private static HttpServer startServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", AwsConfigurationDevModeTest::respond);
            server.start();
            return server;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
            String path = exchange.getRequestURI().getPath();
            String value = Integer.toString(generation.get());
            String body;
            exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.1");
            if ("AmazonSSM.GetParameters".equals(target)) {
                body = "{\"Parameters\":[{\"Name\":\"/ssm\",\"Value\":\"" + value + "\",\"Type\":\"SecureString\"}]}";
            } else if ("AmazonSSM.GetParameter".equals(target)) {
                body = "{\"Parameter\":{\"Name\":\"/ssm\",\"Value\":\"" + value + "\",\"Type\":\"SecureString\"}}";
            } else if ("secretsmanager.GetSecretValue".equals(target)) {
                body = Json.createObjectBuilder().add("SecretString", value).build().toString();
            } else if (path.equals("/bucket/config.properties")) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.getResponseHeaders().set("ETag", "\"" + value + "\"");
                body = "s3=" + value;
            } else if (path.equals("/configurationsessions")) {
                body = "{\"InitialConfigurationToken\":\"token-" + value + "\"}";
            } else if (path.equals("/configuration")) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.getResponseHeaders().set("Next-Poll-Configuration-Token", "next-" + value);
                exchange.getResponseHeaders().set("Next-Poll-Interval-In-Seconds", "60");
                body = "appconfig=" + value;
            } else {
                unexpectedRequests.add(exchange.getRequestMethod() + " " + path + " " + target);
                exchange.sendResponseHeaders(500, -1);
                return;
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }
}
