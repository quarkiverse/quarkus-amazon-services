package io.quarkiverse.amazon.codegen.poet.runtime;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class ConfigClassTest {

    @Test
    public void testConfigClass() {
        validateGeneration(ConfigClass::new, restJsonQuarkusModel(), "test-config-class.java");
        validateGeneration(ConfigClass::new, customNamesQuarkusModel(), "test-config-class-customnames.java");
    }

    static void validateGeneration(Function<QuarkusServiceModel, ClassSpec> generatorConstructor,
            QuarkusServiceModel model,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
