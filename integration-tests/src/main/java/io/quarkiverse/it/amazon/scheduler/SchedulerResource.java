package io.quarkiverse.it.amazon.scheduler;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;

@Path("/scheduler")
public class SchedulerResource {

    private static final Logger LOG = Logger.getLogger(SchedulerResource.class);
    public final static String SCHEDULE_NAME = "quarkus-schedule-";
    public final static String TARGET_ARN = "arn:aws:sqs:us-east-1:000000000000:quarkus-messaging-test-queue";
    public final static String TARGET_ROLE_ARN = "arn:aws:iam::000000000000:role/quarkus-schedule";

    @Inject
    SchedulerClient schedulerClient;

    @Inject
    SchedulerAsyncClient schedulerAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync EventBridge Scheduler client");

        schedulerClient.createSchedule(csr -> csr
                .name(SCHEDULE_NAME + "sync")
                .flexibleTimeWindow(ftw -> ftw.mode(FlexibleTimeWindowMode.OFF))
                .target(t -> t
                        .roleArn(TARGET_ROLE_ARN)
                        .arn(TARGET_ARN)
                        .input("Message for scheduleArn: '<aws.scheduler.schedule-arn>', scheduledTime: '<aws.scheduler.scheduled-time>'"))
                .scheduleExpression("rate(2 minutes)"));
        return schedulerClient.getSchedule(builder -> builder.name(SCHEDULE_NAME + "sync")).arn();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async EventBridge Scheduler client");

        return schedulerAsyncClient
                .createSchedule(csr -> csr
                        .name(SCHEDULE_NAME + "async")
                        .flexibleTimeWindow(ftw -> ftw.mode(FlexibleTimeWindowMode.OFF))
                        .target(t -> t
                                .roleArn(TARGET_ROLE_ARN)
                                .arn(TARGET_ARN)
                                .input("Message for scheduleArn: '<aws.scheduler.schedule-arn>', scheduledTime: '<aws.scheduler.scheduled-time>'"))
                        .scheduleExpression("rate(2 minutes)"))
                .thenCompose(discard -> schedulerAsyncClient.getSchedule(builder -> builder.name(SCHEDULE_NAME + "async"))
                        .thenApply(r -> r.arn()));
    }
}
