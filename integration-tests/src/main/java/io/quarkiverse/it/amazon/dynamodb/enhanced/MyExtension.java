package io.quarkiverse.it.amazon.dynamodb.enhanced;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.it.amazon.dynamodb.DynamoDBUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext.AfterRead;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext.BeforeWrite;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class MyExtension implements software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension {

    @Override
    public ReadModification afterRead(AfterRead context) {
        return DynamoDbEnhancedClientExtension.super.afterRead(context);
    }

    @Override
    public WriteModification beforeWrite(BeforeWrite context) {
        Map<String, AttributeValue> item = new HashMap<>(context.items());

        Map<String, AttributeValue> itemToTransform = new HashMap<>(item);

        itemToTransform.put(DynamoDBUtils.PAYLOAD_NAME, modifyPayload(item.get(DynamoDBUtils.PAYLOAD_NAME)));

        return WriteModification.builder()
                .transformedItem(Collections.unmodifiableMap(itemToTransform))
                .build();
    }

    private AttributeValue modifyPayload(AttributeValue payload) {
        return AttributeValue.builder().s("EXTENSION " + payload.s()).build();
    }
}
