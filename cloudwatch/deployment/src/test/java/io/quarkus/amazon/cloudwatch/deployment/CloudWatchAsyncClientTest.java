package io.quarkus.amazon.cloudwatch.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

public class CloudWatchAsyncClientTest {

    @Inject
    CloudWatchAsyncClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void shouldBeAbleToPutMetricData() {
        // This tests abuses join() since the scope of the test is just to see if async client works

        UUID testExecution = UUID.randomUUID();

        //Given some MetricData

        MetricDatum metricDatum = MetricDatum.builder()
                .timestamp(Instant.now()).metricName("Invocation")
                .unit(StandardUnit.COUNT)
                .value(1.0)
                .build();

        //When that is pushed to AWS CW

        client.putMetricData(PutMetricDataRequest.builder()
                .namespace("Test - " + testExecution)
                .metricData(metricDatum)
                .build()).join();

        //Then it should be able to be read the data back

        ListMetricsResponse metricsResponse = client.listMetrics().join();

        Metric metric = metricsResponse.metrics().stream().filter(
                        value -> value.namespace().equals("Test - " + testExecution)).findAny()
                .orElseGet(() -> fail("Metric must be present"));

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

        GetMetricDataResponse metricData = client.getMetricData(request).join();

        List<MetricDataResult> metricDataResults = metricData.metricDataResults();

        Log.debug(metricDataResults);

        MetricDataResult sumQuery = metricDataResults.stream().filter(
                        result -> result.id().equals("sumQuery")).findAny()
                .orElseGet(() -> fail("Query Must be presented"));

        double sumOfValues = sumQuery.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1, sumOfValues, "Data for the Metric should be exactly 1");

    }
}
