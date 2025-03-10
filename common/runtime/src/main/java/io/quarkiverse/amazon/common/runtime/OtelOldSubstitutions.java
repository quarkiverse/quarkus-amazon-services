package io.quarkiverse.amazon.common.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.Timer;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

public class OtelOldSubstitutions {
    static final String SOFTWARE_AMAZON_AWSSDK_SQS_PACKAGE = "software.amazon.awssdk.services.sqs";
    static final String SOFTWARE_AMAZON_AWSSDK_SNS_PACKAGE = "software.amazon.awssdk.services.sns";
    static final String IO_OPENTELEMETRY_INSTRUMENTATION_AWSSDK_V2_2_PACKAGE = "io.opentelemetry.instrumentation.awssdk.v2_2";
    static final String IO_OPENTELEMETRY_INSTRUMENTATION_AWSSDK_V2_2_INTERNAL_PACKAGE = "io.opentelemetry.instrumentation.awssdk.v2_2.internal";

    static final class IsOtelAwsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(IO_OPENTELEMETRY_INSTRUMENTATION_AWSSDK_V2_2_PACKAGE));
        }
    }

    static final class IsOtelNewAwsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(IO_OPENTELEMETRY_INSTRUMENTATION_AWSSDK_V2_2_INTERNAL_PACKAGE));
        }
    }

    static final class IsSqsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_SQS_PACKAGE));
        }
    }

    static final class IsSnsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return !Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_SNS_PACKAGE));
        }
    }

    static final class IsSqsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_SQS_PACKAGE));
        }
    }

    static final class IsSnsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return Arrays.asList(Package.getPackages()).stream()
                    .map(p -> p.getName())
                    .anyMatch(p -> p.equals(SOFTWARE_AMAZON_AWSSDK_SNS_PACKAGE));
        }
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.Response", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Alias_Response {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsProcessRequest", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Alias_ResponseSqsProcessRequest {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsReceiveRequest", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Alias_ResponseSqsReceiveRequest {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.TracingExecutionInterceptor", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Alias_TracingExecutionInterceptor {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsAccess", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Target_SqsAccess {
        @Delete
        private static boolean enabled;

        @Substitute
        static boolean afterReceiveMessageExecution(
                Context.AfterExecution context,
                ExecutionAttributes executionAttributes,
                Alias_TracingExecutionInterceptor config,
                Timer timer) {
            return false;
        }

        @Substitute
        static SdkRequest modifyRequest(
                SdkRequest request,
                io.opentelemetry.context.Context otelContext,
                boolean useXrayPropagator,
                TextMapPropagator messagingPropagator) {
            return null;
        }

        @Substitute
        static boolean isSqsProducerRequest(SdkRequest request) {
            return false;
        }

        @Substitute
        static String getQueueUrl(SdkRequest request) {
            return null;
        }

        @Substitute
        static String getMessageAttribute(SdkRequest request, String name) {
            return null;
        }

        @Substitute
        static String getMessageId(SdkResponse response) {
            return null;
        }
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkInstrumenterFactory", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Target_AwsSdkInstrumenterFactory {
        @Substitute
        Instrumenter<Alias_ResponseSqsProcessRequest, Alias_Response> consumerProcessInstrumenter() {
            return null;
        }

        @Substitute
        Instrumenter<Alias_ResponseSqsReceiveRequest, Alias_Response> consumerReceiveInstrumenter() {
            return null;
        }
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsImpl", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsImpl {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsMessageImpl", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsMessageImpl {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsParentContext", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsParentContext {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsProcessRequestAttributesGetter", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsProcessRequestAttributesGetter {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsReceiveRequestAttributesGetter", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsReceiveRequestAttributesGetter {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsTracingContext", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SqsTracingContext {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.TracingIterator", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_TracingIterator {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.TracingList", onlyWith = {
            OtelOldSubstitutions.IsSqsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_TracingList {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SnsImpl", onlyWith = {
            OtelOldSubstitutions.IsSnsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    @Delete
    static final class Delete_SnsImpl {
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SnsAccess", onlyWith = {
            OtelOldSubstitutions.IsSnsAbsent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Target_SnsAccess {

        @Delete
        private static boolean enabled;

        @Substitute
        public static SdkRequest modifyRequest(
                SdkRequest request, io.opentelemetry.context.Context otelContext,
                TextMapPropagator messagingPropagator) {
            return null;
        }
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SnsAccess", onlyWith = {
            OtelOldSubstitutions.IsSnsPresent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Target_SnsAccess_Present {

        static {
            enabled = true;
        }

        @Alias
        @RecomputeFieldValue(kind = Kind.FromAlias)
        private static boolean enabled;
    }

    @TargetClass(className = "io.opentelemetry.instrumentation.awssdk.v2_2.SqsAccess", onlyWith = {
            OtelOldSubstitutions.IsSqsPresent.class, OtelOldSubstitutions.IsOtelAwsPresent.class,
            OtelOldSubstitutions.IsOtelNewAwsAbsent.class })
    static final class Target_SqsAccess_Present {

        static {
            enabled = true;
        }

        @Alias
        @RecomputeFieldValue(kind = Kind.FromAlias)
        private static boolean enabled;
    }
}