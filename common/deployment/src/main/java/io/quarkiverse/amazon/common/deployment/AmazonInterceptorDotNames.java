package io.quarkiverse.amazon.common.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import software.amazon.awssdk.awscore.eventstream.EventStreamInitialRequestInterceptor;
import software.amazon.awssdk.awscore.interceptor.GlobalServiceExecutionInterceptor;
import software.amazon.awssdk.awscore.interceptor.HelpfulUnknownHostExceptionInterceptor;
import software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

final class AmazonInterceptorDotNames {

    static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());

    // well-known interceptor
    static final DotName TRACE_ID_EXECUTION_INTERCEPTOR_NAME = DotName
            .createSimple(TraceIdExecutionInterceptor.class.getName());
    static final DotName HELPFUL_UNKNOWN_HOST_EXCEPTION_INTERCEPTOR_NAME = DotName
            .createSimple(HelpfulUnknownHostExceptionInterceptor.class.getName());
    static final DotName GLOBAL_SERVICE_EXECUTION_INTERCEPTOR_NAME = DotName
            .createSimple(GlobalServiceExecutionInterceptor.class.getName());
    static final DotName EVENT_STREAM_INITIAL_REQUEST_INTERCEPTOR_NAME = DotName
            .createSimple(EventStreamInitialRequestInterceptor.class.getName());
    // this interceptor comes from aws-xray-sdk-java which is in maintenance mode and doesn't have new releases
    // we can safely hard code the class name here
    static final DotName XRAY_TRACING_INTERCEPTOR_NAME = DotName
            .createSimple("com.amazonaws.xray.interceptors.TracingInterceptor");

    static final List<DotName> SDK_INTERCEPTOR_LIST = List.of(TRACE_ID_EXECUTION_INTERCEPTOR_NAME,
            HELPFUL_UNKNOWN_HOST_EXCEPTION_INTERCEPTOR_NAME,
            GLOBAL_SERVICE_EXECUTION_INTERCEPTOR_NAME,
            EVENT_STREAM_INITIAL_REQUEST_INTERCEPTOR_NAME);

    static final List<DotName> XRAY_TRACING_INTERCEPTOR_LIST = List.of(XRAY_TRACING_INTERCEPTOR_NAME);

    private AmazonInterceptorDotNames() {
    }

}
