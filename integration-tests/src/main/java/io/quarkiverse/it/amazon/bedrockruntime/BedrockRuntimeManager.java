package io.quarkiverse.it.amazon.bedrockruntime;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

@Singleton
public class BedrockRuntimeManager {
    public static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    private static final String INVOKE_BODY = """
            {"anthropic_version":"bedrock-2023-05-31","max_tokens":100,\
            "messages":[{"role":"user","content":"%s"}]}""";

    @Inject
    BedrockRuntimeClient sync;

    @Inject
    BedrockRuntimeAsyncClient async;

    public String converse(String prompt) {
        return text(sync.converse(converseRequest(prompt)));
    }

    public CompletableFuture<String> converseAsync(String prompt) {
        return async.converse(converseRequest(prompt)).thenApply(BedrockRuntimeManager::text);
    }

    public String invokeModel(String prompt) {
        return sync.invokeModel(invokeModelRequest(prompt)).body().asUtf8String();
    }

    public CompletableFuture<String> invokeModelAsync(String prompt) {
        return async.invokeModel(invokeModelRequest(prompt))
                .thenApply(InvokeModelResponse::body)
                .thenApply(SdkBytes::asUtf8String);
    }

    public CompletableFuture<String> converseStreamAsync(String prompt) {
        final StringBuilder text = new StringBuilder();
        final ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
                .subscriber(ConverseStreamResponseHandler.Visitor.builder()
                        .onContentBlockDelta(event -> text.append(event.delta().text()))
                        .build())
                .build();
        return async.converseStream(ConverseStreamRequest.builder()
                .modelId(MODEL_ID)
                .messages(userMessage(prompt))
                .build(), handler)
                .thenApply(ignore -> text.toString());
    }

    private static ConverseRequest converseRequest(String prompt) {
        return ConverseRequest.builder()
                .modelId(MODEL_ID)
                .messages(userMessage(prompt))
                .build();
    }

    private static Message userMessage(String prompt) {
        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(prompt))
                .build();
    }

    private static InvokeModelRequest invokeModelRequest(String prompt) {
        return InvokeModelRequest.builder()
                .modelId(MODEL_ID)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(INVOKE_BODY.formatted(prompt)))
                .build();
    }

    private static String text(ConverseResponse response) {
        return response.stopReasonAsString() + ":" + response.output().message().content().get(0).text();
    }
}
