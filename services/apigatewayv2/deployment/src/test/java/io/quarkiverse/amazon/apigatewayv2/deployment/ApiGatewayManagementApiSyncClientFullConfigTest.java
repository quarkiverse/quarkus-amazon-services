package io.quarkiverse.amazon.apigatewayv2.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.apigatewayv2.ApiGatewayV2AsyncClient;
import software.amazon.awssdk.services.apigatewayv2.ApiGatewayV2Client;

public class ApiGatewayManagementApiSyncClientFullConfigTest {

    @Inject
    ApiGatewayV2Client client;

    @Inject
    ApiGatewayV2AsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
