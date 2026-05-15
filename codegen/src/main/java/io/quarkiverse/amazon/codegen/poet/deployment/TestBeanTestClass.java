package io.quarkiverse.amazon.codegen.poet.deployment;

import static javax.lang.model.element.Modifier.PUBLIC;

import java.util.Set;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class TestBeanTestClass extends AbstractTestBeanTestClass {
    private final ClassName beanClassName;
    private final String beanName = "bean";

    public TestBeanTestClass(IntermediateModel model) {
        super(model, "BeanTest");

        this.beanClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "Bean");
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
     * Add the test classes
     *
     * @return A list of test classes.
     */
    @Override
    protected Set<ClassName> getTestAdditionalClasses() {
        return Set.of(
                beanClassName);
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
