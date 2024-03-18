package io.quarkus.it.amazon.cloudwatchslogs;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

@Path("/cloudwatchlogs")
public class CloudWatchLogsResource {

    @Inject
    CloudWatchLogsClient client;

    @Inject
    CloudWatchLogsAsyncClient asyncClient;

    @POST
    @Path("/sync/put")
    @Produces(TEXT_PLAIN)
    public String putLogEvent(@FormParam("logGroup") String logGroup,
            @FormParam("logStream") String logStream,
            @FormParam("message") String message) {

        if (!logGroupExists(logGroup)) {
            client.createLogGroup(CreateLogGroupRequest.builder()
                    .logGroupName(logGroup).build());
            awaitLogGroupCreation(logGroup);
        }

        if (!logStreamExists(logGroup, logStream)) {
            client.createLogStream(CreateLogStreamRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream).build());

            awaitLogStreamCreation(logGroup, logStream);
        }

        //
        client.putLogEvents(
                PutLogEventsRequest.builder().logGroupName(logGroup).logStreamName(logStream).logEvents(
                        InputLogEvent.builder()
                                .timestamp(Instant.now().toEpochMilli())
                                .message(message)
                                .build())
                        .build());
        return "OK";
    }

    @GET
    @Path("/sync/get")
    @Produces(TEXT_PLAIN)
    public String queryEvents(@QueryParam("logGroup") String logGroup,
            @QueryParam("logStream") String logStream) {

        //This will retry the query until you get an event (Returns only Non-Empty Lists)
        //Makes easier for tests
        List<String> events = awaitForEventToBeAddedToStream(logGroup, logStream);

        return String.join("\n", events);
    }

    private boolean logStreamExists(String expectedLogGroupName, String expectedLogStreamName) {
        List<LogStream> logGroups = client.describeLogStreams(
                DescribeLogStreamsRequest.builder().logGroupName(expectedLogGroupName).build()).logStreams();

        Optional<LogStream> maybeLogGroup = logGroups.stream()
                .filter(value -> value.logStreamName().equals(expectedLogStreamName))
                .findAny();

        return maybeLogGroup.isPresent();

    }

    private boolean logGroupExists(String logGroup) {
        List<LogGroup> logGroups = client.describeLogGroups().logGroups();

        Optional<LogGroup> maybeLogGroup = logGroups.stream()
                .filter(value -> value.logGroupName().equals(logGroup))
                .findAny();

        return maybeLogGroup.isPresent();

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
        }).onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));
        if (logGroup == null) {
            throw new IllegalStateException("LogGroup was not created " + expectedLogGroupName);
        }

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

        ).onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));

        if (logStream == null) {
            throw new IllegalStateException("LogStream was not created " + expectedLogStreamName);
        }

    }

    private List<String> awaitForEventToBeAddedToStream(String logGroupName, String logStream) {

        List<OutputLogEvent> results = Uni.createFrom().item(() -> {

            GetLogEventsResponse queryResults = client.getLogEvents(GetLogEventsRequest.builder()
                    .logGroupName(logGroupName).logStreamName(logStream).build());

            if (queryResults.events().isEmpty()) {
                throw new IllegalStateException();
            }

            return queryResults.events();
        }).onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .await().atMost(Duration.ofSeconds(10));

        return results.stream().map(OutputLogEvent::message).collect(Collectors.toList());
    }

    // end sync

    @POST
    @Path("/async/put")
    @Produces(TEXT_PLAIN)
    public Uni<String> putLogEventAsync(@FormParam("logGroup") String logGroup,
            @FormParam("logStream") String logStream,
            @FormParam("message") String message) {

        Uni<LogGroup> logGroupCreation = Uni.createFrom().deferred(() -> logGroupExistsAsync(logGroup)
                .replaceWithVoid().onFailure(IllegalStateException.class)
                .recoverWithUni(() -> createLogGroupAsync(logGroup))
                .chain(() -> awaitLogGroupCreationAsync(logGroup)));

        Uni<LogStream> logStreamCreation = Uni.createFrom().deferred(() -> logStreamExistsAsync(logGroup, logStream)
                .replaceWithVoid().onFailure(IllegalStateException.class)
                .recoverWithUni(() -> createLogStreamAsync(logGroup, logStream))
                .chain(() -> awaitLogStreamCreationAsync(logGroup, logStream)));

        Uni<PutLogEventsResponse> putLogEventOperation = Uni.createFrom().deferred(() ->

        Uni.createFrom().completionStage(() -> {

            InputLogEvent logEvent = InputLogEvent.builder()
                    .timestamp(Instant.now().toEpochMilli())
                    .message(message)
                    .build();
            PutLogEventsRequest putLogEventsRequest = PutLogEventsRequest.builder().logGroupName(logGroup)
                    .logStreamName(logStream)
                    .logEvents(logEvent)
                    .build();
            return asyncClient.putLogEvents(putLogEventsRequest);
        }));

        //

        return logGroupCreation
                .chain(() -> logStreamCreation)
                .chain(() -> putLogEventOperation)
                .map(response -> "OK");

    }

    @GET
    @Path("/async/get")
    @Produces(TEXT_PLAIN)
    public Uni<String> queryEventsAsync(@QueryParam("logGroup") String logGroup,
            @QueryParam("logStream") String logStream) {

        //This will retry the query until you get an event (Returns only Non-Empty Lists)
        //Makes easier for tests

        return awaitForEventToBeAddedToStreamAsync(logGroup, logStream).map(
                events -> String.join("\n", events));
    }

    private Uni<Boolean> logGroupExistsAsync(String logGroup) {

        return Uni.createFrom().completionStage(

                asyncClient::describeLogGroups)
                .map(response -> {
                    List<LogGroup> logGroups = response.logGroups();
                    Optional<LogGroup> maybeLogGroup = logGroups.stream()
                            .filter(value -> value.logGroupName().equals(logGroup))
                            .findAny();

                    return maybeLogGroup.isPresent();
                }).flatMap(exists -> {

                    if (exists) {
                        return Uni.createFrom().item(true);
                    } else {
                        return Uni.createFrom().failure(IllegalStateException::new);
                    }

                });

    }

    private Uni<Boolean> logStreamExistsAsync(String expectedLogGroupName, String expectedLogStreamName) {

        return Uni.createFrom().completionStage(

                asyncClient.describeLogStreams(
                        DescribeLogStreamsRequest.builder().logGroupName(expectedLogGroupName).build())

        ).map(response -> {
            List<LogStream> logGroups = response.logStreams();
            Optional<LogStream> maybeLogStream = logGroups.stream()
                    .filter(value -> value.logStreamName().equals(expectedLogStreamName))
                    .findAny();

            return maybeLogStream.isPresent();
        }).flatMap(exists -> {

            if (exists) {
                return Uni.createFrom().item(true);
            } else {
                return Uni.createFrom().failure(IllegalStateException::new);
            }

        });
    }

    private Uni<Void> createLogGroupAsync(String logGroup) {
        return Uni.createFrom().completionStage(() -> asyncClient.createLogGroup(CreateLogGroupRequest.builder()
                .logGroupName(logGroup).build())).replaceWithVoid();
    }

    private Uni<Void> createLogStreamAsync(String logGroup, String logStream) {
        return Uni.createFrom().completionStage(() -> asyncClient.createLogStream(CreateLogStreamRequest.builder()
                .logGroupName(logGroup).logStreamName(logStream).build())).replaceWithVoid();
    }

    private Uni<LogGroup> awaitLogGroupCreationAsync(String expectedLogGroupName) {

        return Uni.createFrom().completionStage(asyncClient::describeLogGroups)
                .map(response -> {
                    List<LogGroup> logGroups = response.logGroups();
                    return logGroups.stream()
                            .filter(value -> value.logGroupName().equals(expectedLogGroupName))
                            .findAny();
                }).flatMap(exists -> {
                    if (exists.isPresent()) {
                        return Uni.createFrom().item(exists::get);
                    } else {
                        return Uni.createFrom().failure(IllegalStateException::new);
                    }
                })
                .onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .onItem().ifNull()
                .failWith(() -> new IllegalStateException("LogGroup was not created " + expectedLogGroupName));

    }

    private Uni<LogStream> awaitLogStreamCreationAsync(String expectedLogGroupName,
            String expectedLogStreamName) {

        return Uni.createFrom().completionStage(() -> asyncClient.describeLogStreams(
                DescribeLogStreamsRequest.builder()
                        .logGroupName(expectedLogGroupName)
                        .build()))
                .map(response -> {
                    List<LogStream> logStreams = response.logStreams();
                    return logStreams.stream()
                            .filter(value -> value.logStreamName().equals(expectedLogStreamName))
                            .findAny();
                }).flatMap(exists -> {

                    if (exists.isPresent()) {
                        return Uni.createFrom().item(exists::get);
                    } else {
                        return Uni.createFrom().failure(IllegalStateException::new);
                    }
                })
                .onFailure(IllegalStateException.class)
                .retry().indefinitely()
                .onItem().ifNull()
                .failWith(() -> new IllegalStateException("LogStream was not created " + expectedLogStreamName));

    }

    private Uni<List<String>> awaitForEventToBeAddedToStreamAsync(String logGroupName, String logStream) {

        return Uni.createFrom().completionStage(() -> asyncClient.getLogEvents(GetLogEventsRequest.builder()
                .logGroupName(logGroupName).logStreamName(logStream).build()))

                .flatMap(response -> {

                    if (!response.events().isEmpty()) {
                        return Uni.createFrom().item(response::events);
                    } else {
                        return Uni.createFrom().failure(IllegalStateException::new);
                    }
                }).onFailure(IllegalStateException.class)
                .retry().indefinitely().map(events -> events.stream()
                        .map(OutputLogEvent::message)
                        .collect(Collectors.toList()));

    }

}
