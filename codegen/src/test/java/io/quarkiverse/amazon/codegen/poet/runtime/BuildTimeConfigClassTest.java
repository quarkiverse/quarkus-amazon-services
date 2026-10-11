package io.quarkiverse.amazon.codegen.poet.runtime;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonServiceModels;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class BuildTimeConfigClassTest {

    @Test
    public void testBuildTimeConfigClass() {
        validateGeneration(BuildTimeConfigClass::new, restJsonServiceModels(), false, "test-buildtimeconfig-class.java");
        validateGeneration(BuildTimeConfigClass::new, restJsonServiceModels(), true,
                "test-buildtimeconfig-class-customdevservices.java");
    }

    static void validateGeneration(BiFunction<IntermediateModel, Boolean, ClassSpec> generatorConstructor,
            IntermediateModel model,
            boolean hasCustomDevServicesBuildTimeConfig,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model, hasCustomDevServicesBuildTimeConfig), generatesTo(expectedClassName));
    }
}
