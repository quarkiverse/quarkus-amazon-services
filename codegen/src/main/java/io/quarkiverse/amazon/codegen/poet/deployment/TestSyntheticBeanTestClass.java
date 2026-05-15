package io.quarkiverse.amazon.codegen.poet.deployment;

import static javax.lang.model.element.Modifier.PUBLIC;

import java.util.Set;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * This approach is challenging. The key issue is that the static bean generator function
 * is recorded as a static value. This is then replayed, but the Instance class has already
 * been injected as a static value. Therefore, there is no injection point.
 */
public class TestSyntheticBeanTestClass extends AbstractTestBeanTestClass {
    private final ClassName beanClassName;
    private final String beanName = "bean";

    public TestSyntheticBeanTestClass(IntermediateModel model) {
        super(model, "SyntheticBeanTest");

        this.beanClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "SyntheticBean");
    }

    /**
     * Add the injected bean
     *
     * @param classBuilder The builder for the class.
     */
    @Override
    protected void customiseClass(TypeSpec.Builder classBuilder) {
        classBuilder.addField(
                FieldSpec.builder(beanClassName, beanName)
                        .addAnnotation(jakartaInjectClassName)
                        .build());
    }

    /**
     * Add the test processors
     *
     * @return A list of test processors.
     */
    @Override
    protected Set<ClassName> getTestProcessors() {
        return Set.of(
                ClassName.get(this.quarkusDeploymentPackage, model.getMetadata().getServiceName() + "TestProcessor"));
    }

    /**
     * A simple test that uses runtime config to verify that a client has been created.
     */
    MethodSpec addFullConfigTest() {
        return MethodSpec.methodBuilder("fullConfig")
                .addModifiers(PUBLIC)
                .addAnnotation(junitTestClassName)
                .addStatement("$T.assertNotNull($N)", junitAssertionsClassName, beanName)
                .addStatement("$T.assertDoesNotThrow(() -> { $N.invokeAsyncClient(); })", junitAssertionsClassName, beanName)
                .addStatement("$T.assertDoesNotThrow(() -> { $N.invokeSyncClient(); })", junitAssertionsClassName, beanName)
                .build();
    }

    @Override
    public ClassName className() {
        return testClassName;
    }
}
