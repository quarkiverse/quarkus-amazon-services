package io.quarkus.it.amazon.cloudwatch;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.time.Instant;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.cloudwatch.paginators.ListMetricsIterable;

@Path("/cloudwatch")
public class CloudWatchResource {
    @Inject
    CloudWatchClient client;

    @Inject
    CloudWatchAsyncClient asyncClient;

    @POST
    @Path("/sync")
    @Produces(TEXT_PLAIN)
    public String testSync(@FormParam("namespace") String namespace) {

        // Push data to CW

        MetricDatum metricDatum = MetricDatum.builder()
                .timestamp(Instant.now()).metricName("Invocation")
                .unit(StandardUnit.COUNT)
                .value(1.0)
                .build();

        client.putMetricData(PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(metricDatum)
                .build());

        // Read back and return the data

        ListMetricsIterable metricsIterable = client.listMetricsPaginator();

        Metric metric = metricsIterable.metrics().stream().filter(
                value -> value.namespace().equals(namespace)).findAny()
                .orElseThrow(() -> new IllegalStateException(namespace + " was not found"));

        Log.debug("Metric: " + metric);

        Instant queryTime = Instant.now();

        MetricStat metStat = MetricStat.builder()
                .stat("Sum")
                .period(1)
                .metric(metric)
                .build();

        MetricDataQuery dataQuery = MetricDataQuery.builder()
                .metricStat(metStat)
                .id("sumQuery")
                .returnData(true)
                .build();

        GetMetricDataRequest request = GetMetricDataRequest.builder()
                .maxDatapoints(10)
                .scanBy(ScanBy.TIMESTAMP_DESCENDING)
                .startTime(queryTime.minusSeconds(10))
                .endTime(queryTime.plusSeconds(10))
                .metricDataQueries(dataQuery)
                .build();

        GetMetricDataResponse metricData = client.getMetricData(request);

        List<MetricDataResult> metricDataResults = metricData.metricDataResults();

        Log.debug(metricDataResults);

        MetricDataResult sumQuery = metricDataResults.stream().filter(
                result -> result.id().equals("sumQuery")).findAny()
                .orElseThrow(() -> new IllegalStateException(namespace + " was not found"));

        double sumOfValues = sumQuery.values().stream().mapToDouble(Double::doubleValue).sum();

        return "Sum of Invocations for " + namespace + " is " + sumOfValues;

    }

    @POST
    @Path("/async")
    @Produces(TEXT_PLAIN)
    public Uni<String> testAsync(@FormParam("namespace") String namespace) {

        //Upset metrics and list every metric
        Multi<Metric> metrics = putMetric(namespace)
                .toMulti()
                .flatMap(putMetricDataResponse -> listMetrics());

        //Lookup the metrics and query the sum of it
        return findMetricByNamespace(metrics, namespace).flatMap(
                this::querySumOfMetricData);

    }

    //These are just helpers methods to help me chain the monads/unis

    private Uni<PutMetricDataResponse> putMetric(String namespace) {
        return Uni.createFrom().completionStage(() -> {
            MetricDatum metricDatum = MetricDatum.builder()
                    .timestamp(Instant.now()).metricName("Invocation")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .build();

            return asyncClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(metricDatum)
                    .build());
        });
    }

    private Multi<Metric> listMetrics() {
        return Multi.createFrom().emitter(emitter -> {
            ListMetricsIterable metricsIterable = client.listMetricsPaginator();
            metricsIterable.metrics().forEach(emitter::emit);
        });

    }

    private Uni<Metric> findMetricByNamespace(Multi<Metric> source, String namespace) {
        return source.filter(value -> value.namespace().equals(namespace)).collect().first()
                .onItem().ifNull().failWith(
                        () -> new IllegalStateException(namespace + " was not found"));
    }

    private Uni<String> querySumOfMetricData(Metric metric) {
        Uni<GetMetricDataResponse> query = Uni.createFrom().completionStage(() -> {

            Instant queryTime = Instant.now();

            MetricStat metStat = MetricStat.builder()
                    .stat("Sum")
                    .period(1)
                    .metric(metric)
                    .build();

            MetricDataQuery dataQuery = MetricDataQuery.builder()
                    .metricStat(metStat)
                    .id("sumQuery")
                    .returnData(true)
                    .build();

            GetMetricDataRequest request = GetMetricDataRequest.builder()
                    .maxDatapoints(10)
                    .scanBy(ScanBy.TIMESTAMP_DESCENDING)
                    .startTime(queryTime.minusSeconds(10))
                    .endTime(queryTime.plusSeconds(10))
                    .metricDataQueries(dataQuery)
                    .build();

            return asyncClient.getMetricData(request);
        });

        return query.toMulti().flatMap(response -> Multi.createFrom().items(
                response.metricDataResults())).onItem().<MetricDataResult> disjoint().filter(
                        result -> result.id().equals("sumQuery"))
                .collect().first().onItem().ifNull().failWith(
                        () -> new IllegalStateException(metric.namespace() + " was not found"))
                .map(result -> {

                    double sumOfValues = result.values().stream().mapToDouble(Double::doubleValue).sum();

                    return "Sum of Invocations for " + metric.namespace() + " is " + sumOfValues;

                });

    }
}
