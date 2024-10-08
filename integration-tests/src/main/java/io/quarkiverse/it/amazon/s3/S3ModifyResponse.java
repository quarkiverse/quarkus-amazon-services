package io.quarkiverse.it.amazon.s3;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context.ModifyResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3ModifyResponse implements ExecutionInterceptor {
    final static String CUSTOM_METADATA = "CUSTOM-METADATA";

    private String value;

    public S3ModifyResponse() {
        this("DEFAULT_CONSTRUCTOR");
    }

    public S3ModifyResponse(String value) {
        this.value = value;
    }

    @Override
    public SdkResponse modifyResponse(ModifyResponse context, ExecutionAttributes executionAttributes) {
        if (context.response() instanceof GetObjectResponse) {
            GetObjectResponse response = (GetObjectResponse) context.response();
            return response.copy(r -> r.metadata(updateMetadata(response.metadata())));
        }
        return context.response();
    }

    private Map<String, String> updateMetadata(Map<String, String> metadata) {
        Map<String, String> modifiedMetadata = new HashMap<>(metadata);

        modifiedMetadata.put(CUSTOM_METADATA, value);

        return modifiedMetadata;
    }
}
