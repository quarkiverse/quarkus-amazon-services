package io.quarkus.it.amazon.lambda;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.utils.StringUtils;

/*
 * Created by triphon 21.11.23 г.
 */
@ApplicationScoped
public class LambdaUtils {
    private static final Logger LOG = Logger.getLogger(LambdaResource.class);
    public static final String LAMBDA_PREFIX = "hello-lambda";
    @Inject
    LambdaClient lambdaClient;
    @Inject
    LambdaAsyncClient lambdaAsyncClient;

    private String lambdaName;

    @PostConstruct
    void init() {
        lambdaName = MessageFormat.format("{0}-{1,number,#}", LAMBDA_PREFIX, System.currentTimeMillis() % 1000000000);
    }

    void createLambda(byte[] function) {
        LOG.info("createLambda start");
        lambdaClient.createFunction(b -> b.functionName(getLambdaName())
                .runtime(Runtime.NODEJS18_X)
                .handler("index.handler")
                .role("arn:aws:iam::000000000000:role/lambda-role")
                .code(c -> c.zipFile(SdkBytes
                        .fromByteArray(function))));
        LOG.info("createLambda end");
    }

    FunctionConfiguration findDevLambdaBlocking() {
        final ListFunctionsResponse listFunctionsResponse = lambdaClient.listFunctions();
        return extractDevLambda(listFunctionsResponse);
    }

    CompletableFuture<FunctionConfiguration> findDevLambdaAsync() {
        return lambdaAsyncClient.listFunctions()
                .thenApply(listFunctionsResponse -> extractDevLambda(listFunctionsResponse));
    }

    private FunctionConfiguration extractDevLambda(ListFunctionsResponse listFunctionsResponse) {
        return listFunctionsResponse.functions().stream()
                .filter(functionConfiguration -> StringUtils.equals(getLambdaName(), functionConfiguration.functionName()))
                .findFirst().orElseThrow();
    }

    String getLambdaName() {
        return lambdaName;
    }
}
