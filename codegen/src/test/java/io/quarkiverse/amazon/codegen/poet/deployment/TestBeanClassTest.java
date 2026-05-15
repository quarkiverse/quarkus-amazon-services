package io.quarkiverse.amazon.codegen.poet.deployment;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonServiceModels;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestBeanClassTest {

    @Test
    public void testClass() {
        validateGeneration(TestBeanTestClass::new, restJsonServiceModels(), "test-bean-test-class.java");
    }

    static void validateGeneration(Function<IntermediateModel, ClassSpec> generatorConstructor,
            IntermediateModel model,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
