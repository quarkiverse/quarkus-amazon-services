package io.quarkus.amazon.cloudwatch.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;

@ApplicationScoped
public class CloudWatchLogsClientProducer {
    private final CloudWatchLogsClient syncClient;
    private final CloudWatchLogsAsyncClient asyncClient;

    CloudWatchLogsClientProducer(Instance<CloudWatchLogsClientBuilder> syncClientBuilderInstance,
            Instance<CloudWatchLogsAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public CloudWatchLogsClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The CloudWatchClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public CloudWatchLogsAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The CloudWatchAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
