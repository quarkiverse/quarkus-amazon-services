package io.quarkiverse.amazon.codegen.poet.devservices;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class DevServicesProcessorClassTest {

    @Test
    public void testDevServicesProcessorClass() {
        assertThat(new DevServicesProcessorClass(restJsonQuarkusModel()),
                generatesTo("test-devservices-processor-class.java"));
        assertThat(new DevServicesProcessorClass(customNamesQuarkusModel()),
                generatesTo("test-devservices-processor-class-customnames.java"));
    }
}
