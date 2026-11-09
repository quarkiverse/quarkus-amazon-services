package io.quarkiverse.amazon.codegen.poet.runtime;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class RecorderClassTest {

    @Test
    public void testRecorderClass() {
        validateGeneration(RecorderClass::new, restJsonQuarkusModel(), "test-recorder-class.java");
    }

    @Test
    void endpointDiscoveryIsConfiguredOnNamedSyncAndAsyncBuilders() {
        var model = restJsonQuarkusModel();
        model.intermediateModel().setEndpointOperation(new OperationModel());
        String source = new RecorderClass(model).poetSpec().toString().replaceAll("\\s+", "");
        assertThat(source, containsString("getSyncBuilder(java.lang.StringclientName)"));
        assertThat(source, containsString("getAsyncBuilder(java.lang.StringclientName)"));
        assertThat(source, containsString("enableEndpointDiscovery(clientName).ifPresent(builder::endpointDiscoveryEnabled)"));
    }

    static void validateGeneration(Function<QuarkusServiceModel, ClassSpec> generatorConstructor,
            QuarkusServiceModel model,
            String expectedClassName) {

        assertThat(generatorConstructor.apply(model), generatesTo(expectedClassName));
    }
}
