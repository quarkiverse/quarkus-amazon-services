package io.quarkiverse.amazon.codegen.poet.runtime;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonServiceModels;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class ConfigClassTest {

    @Test
    public void testConfigClass() {
        validateGeneration(ConfigClass::new, restJsonServiceModels(), "test-config-class.java");
    }

    static void validateGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
            IntermediateModel model,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
