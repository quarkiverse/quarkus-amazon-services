package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class DynamoDbEnhancedDbTableNotABeanTest {

    @NamedDynamoDbTable("sync")
    @Inject
    DynamoDbTable<DynamoDBExampleTableNotABean> syncTable;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClass(DynamoDBExampleTableNotABean.class)
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
