package io.quarkus.it.amazon;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class Profiles {

    public static class DynamoDBEnhancedClientWithoutCustomExtensions implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.dynamodbenhanced.client-extensions", "");
        }

        @Override
        public String getConfigProfile() {
            return "test-dynamodb-enhanced-client-without-custom-extensions";
        }
    }

}
