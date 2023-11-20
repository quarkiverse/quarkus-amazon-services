package io.quarkus.amazon.lambdaclient.deployment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class LambdaDevServicesTest {

    public static final String HELLO_LAMBDA = "hello-lambda";
    public static final String BODY = "body";
    @Inject
    Instance<LambdaClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.aws.devservices.localstack.image-name=localstack/localstack:2.3.0"),
                            "application.properties"));

    @Test
    public void test() {
        assertNotNull(client.get());
        Assertions.assertEquals(Set.of(HELLO_LAMBDA),
                client.get().listFunctions().functions().stream().map(FunctionConfiguration::functionName)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void invoke() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        assertNotNull(client.get());
        final FunctionConfiguration functionInfo = assertDoesNotThrow(() -> client.get().listFunctions().functions().stream()
                .filter(functionConfiguration -> StringUtils.equals(HELLO_LAMBDA, functionConfiguration.functionName()))
                .findFirst().orElseThrow());
        final InvokeResponse response = invokeLambda(functionInfo, objectMapper);
        final String responseAsString = response.payload().asUtf8String();
        final Map<String, Object> responseMap = objectMapper.readValue(responseAsString,
                new TypeReference<Map<String, Object>>() {
                });
        Assertions.assertEquals("Hello World!", responseMap.get(BODY));
    }

    private InvokeResponse invokeLambda(FunctionConfiguration functionInfo, ObjectMapper objectMapper)
            throws InterruptedException {
        try {
            return client.get().invoke(builder -> {
                try {
                    builder.functionName(functionInfo.functionArn())
                            .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(Map
                                    .of(BODY, "{\"name\":\"World\"}"))));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            if (e.getMessage().contains(" The function is currently in the following state: Pending")) {
                Thread.sleep(1000);
                return invokeLambda(functionInfo, objectMapper);
            }
            throw new RuntimeException(e);
        }
    }
}
