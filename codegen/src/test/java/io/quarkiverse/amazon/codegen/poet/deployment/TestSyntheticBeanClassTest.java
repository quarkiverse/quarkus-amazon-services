package io.quarkiverse.amazon.codegen.poet.deployment;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestSyntheticBeanClassTest {

    @Test
    public void testClass() {
        validateGeneration(TestSyntheticBeanTestClass::new, restJsonQuarkusModel(), "test-syntheticbean-test-class.java");
    }

    static void validateGeneration(Function<QuarkusServiceModel, ClassSpec> generatorConstructor,
            QuarkusServiceModel model,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
