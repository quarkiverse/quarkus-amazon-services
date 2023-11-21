package io.quarkus.it.amazon.lambda;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.utils.StringUtils;

/*
 * Created by triphon 21.11.23 г.
 */
@ApplicationScoped
public class LambdaUtils {
    public static final String LAMBDA = "hello-lambda";
    public static final String PAYLOAD = "{\"body\":\"{\\\"name\\\":\\\"World\\\"}\"}";
    @Inject
    LambdaClient lambdaClient;

    FunctionConfiguration findDevLambda() {
        return lambdaClient.listFunctions().functions().stream()
                .filter(functionConfiguration -> StringUtils.equals(LAMBDA, functionConfiguration.functionName()))
                .findFirst().orElseThrow();
    }

    InvokeResponse invokeLambda(FunctionConfiguration functionInfo)
            throws InterruptedException {
        try {
            return lambdaClient.invoke(builder -> getPayloadBuilder(functionInfo, builder));
        } catch (Exception e) {
            if (e.getMessage().contains(" The function is currently in the following state: Pending")) {
                Thread.sleep(1000);
                return invokeLambda(functionInfo);
            }
            throw new RuntimeException(e);
        }
    }

    static InvokeRequest.Builder getPayloadBuilder(FunctionConfiguration functionInfo, InvokeRequest.Builder builder) {
        return builder.functionName(functionInfo.functionArn())
                .payload(SdkBytes.fromUtf8String(PAYLOAD));
    }
}
