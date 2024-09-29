package io.quarkus.amazon.lambdaclient.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;

public class LambdaDevServicesTest {

    public static final String HELLO_LAMBDA = "hello-lambda";
    public static final String BODY = "body";
    @Inject
    Instance<LambdaClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void test() {
        assertNotNull(client.get());
        Assertions.assertDoesNotThrow(
                () -> client.get().listFunctions().functions().stream().map(FunctionConfiguration::functionName)
                        .collect(Collectors.toSet()));
    }
}
