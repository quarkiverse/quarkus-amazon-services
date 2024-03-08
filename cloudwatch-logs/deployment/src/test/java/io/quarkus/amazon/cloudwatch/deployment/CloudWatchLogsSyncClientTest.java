package io.quarkus.amazon.cloudwatch.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

public class CloudWatchLogsSyncClientTest {

    @Inject
    CloudWatchLogsClient client;

    @Inject
    CloudWatchLogsAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    @Timeout(value = 30)
    public void test() {
        // Given some log stream

        String logGroup = UUID.randomUUID().toString();
        String logStream = UUID.randomUUID().toString();

        client.createLogGroup(CreateLogGroupRequest.builder()
                .logGroupName(logGroup).build());

        awaitLogGroupCreation(logGroup);

        client.createLogStream(CreateLogStreamRequest.builder()
                .logGroupName(logGroup)
                .logStreamName(logStream).build());

        awaitLogStreamCreation(logGroup, logStream);

        // When some logs are pushed to AWS CW Logs

        client.putLogEvents(
                PutLogEventsRequest.builder().logGroupName(logGroup).logStreamName(logStream).logEvents(
                        InputLogEvent.builder()
                                .timestamp(Instant.now().toEpochMilli())
                                .message("Hello World!")
                                .build())
                        .build());

        // Then those should be readable from AWS CW Logs

        List<String> messages = awaitForEventToBeAddedToStream(logGroup, logStream);

        assertEquals(List.of("Hello World!"), messages);

    }

    private List<String> awaitForEventToBeAddedToStream(String logGroupName, String logStream) {

        List<OutputLogEvent> results = Uni.createFrom().item(() -> {

            GetLogEventsResponse queryResults = client.getLogEvents(GetLogEventsRequest.builder()
                    .logGroupName(logGroupName).logStreamName(logStream).build());

            if (queryResults.events().isEmpty()) {
                throw new IllegalStateException();
            }

            return queryResults.events();
        }).log().onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .onFailure(ResourceNotFoundException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));

        assertNotNull(results);

        return results.stream().map(OutputLogEvent::message).collect(Collectors.toList());
    }

    private void awaitLogGroupCreation(String expectedLogGroupName) {

        LogGroup logGroup = Uni.createFrom().item(() -> {
            List<LogGroup> logGroups = client.describeLogGroups().logGroups();

            Optional<LogGroup> maybeLogGroup = logGroups.stream()
                    .filter(value -> value.logGroupName().equals(expectedLogGroupName))
                    .findAny();

            if (maybeLogGroup.isEmpty()) {
                throw new IllegalStateException();
            }

            return maybeLogGroup.get();
        }

        ).log().onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));

        assertNotNull(logGroup);

    }

    private void awaitLogStreamCreation(String expectedLogGroupName, String expectedLogStreamName) {

        LogStream logStream = Uni.createFrom().item(() -> {
            List<LogStream> logGroups = client.describeLogStreams(
                    DescribeLogStreamsRequest.builder().logGroupName(expectedLogGroupName).build()).logStreams();

            Optional<LogStream> maybeLogGroup = logGroups.stream()
                    .filter(value -> value.logStreamName().equals(expectedLogStreamName))
                    .findAny();

            if (maybeLogGroup.isEmpty()) {
                throw new IllegalStateException();
            }

            return maybeLogGroup.get();
        }

        ).log().onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));

        assertNotNull(logStream);

    }
}
