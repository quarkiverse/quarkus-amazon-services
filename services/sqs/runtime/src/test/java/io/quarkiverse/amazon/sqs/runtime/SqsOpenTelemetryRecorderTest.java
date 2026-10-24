package io.quarkiverse.amazon.sqs.runtime;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsOpenTelemetryRecorderTest {

    private final SqsOpenTelemetryRecorder recorder = new SqsOpenTelemetryRecorder();
    private final AwsSdkTelemetry telemetry = AwsSdkTelemetry.create(OpenTelemetry.noop());

    @Test
    void syncWrapperSettersReturnWrapper() {
        AwsClientBuilder<?, ?> wrapper = recorder.wrapSyncClientBuilder(SqsClient.builder(), telemetry);
        assertSame(wrapper, wrapper.region(Region.US_EAST_1));
        assertSame(wrapper, wrapper.credentialsProvider(AnonymousCredentialsProvider.create()));
    }

    @Test
    void asyncWrapperSettersReturnWrapper() {
        AwsClientBuilder<?, ?> wrapper = recorder.wrapAsyncClientBuilder(SqsAsyncClient.builder(), telemetry);
        assertSame(wrapper, wrapper.region(Region.US_EAST_1));
        assertSame(wrapper, wrapper.credentialsProvider(AnonymousCredentialsProvider.create()));
    }
}
