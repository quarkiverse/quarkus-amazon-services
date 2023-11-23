package io.quarkus.it.amazon.lambda;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.utils.StringUtils;

/*
 * Created by triphon 21.11.23 г.
 */
@ApplicationScoped
public class LambdaUtils {
    public static final String LAMBDA = "hello-lambda";
    @Inject
    LambdaClient lambdaClient;
    @Inject
    LambdaAsyncClient lambdaAsyncClient;

    FunctionConfiguration findDevLambdaBlocking() {
        final ListFunctionsResponse listFunctionsResponse = lambdaClient.listFunctions();
        return extractDevLambda(listFunctionsResponse);
    }

    CompletableFuture<FunctionConfiguration> findDevLambdaAsync() {
        return lambdaAsyncClient.listFunctions()
                .thenApply(listFunctionsResponse -> extractDevLambda(listFunctionsResponse));
    }

    private static FunctionConfiguration extractDevLambda(ListFunctionsResponse listFunctionsResponse) {
        return listFunctionsResponse.functions().stream()
                .filter(functionConfiguration -> StringUtils.equals(LAMBDA, functionConfiguration.functionName()))
                .findFirst().orElseThrow();
    }
}
