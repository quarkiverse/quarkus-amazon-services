package io.quarkus.amazon.dynamodb.enhanced.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Recorder
public class DynamodbEnhancedClientRecorder {

    public BeanContainerListener setDynamoDbClient() {
        BeanContainerListener beanContainerListener = new BeanContainerListener() {

            @Override
            public void created(BeanContainer container) {
                DynamodbEnhancedClientProducer producer = container.beanInstance(DynamodbEnhancedClientProducer.class);
                producer.setDynamoDbClient(container.beanInstance(DynamoDbClient.class));
            }
        };

        return beanContainerListener;
    }

    public BeanContainerListener setDynamoDbAsyncClient() {
        BeanContainerListener beanContainerListener = new BeanContainerListener() {

            @Override
            public void created(BeanContainer container) {
                DynamodbEnhancedClientProducer producer = container.beanInstance(DynamodbEnhancedClientProducer.class);
                producer.setDynamoDbAsyncClient(container.beanInstance(DynamoDbAsyncClient.class));
            }
        };

        return beanContainerListener;
    }
}
