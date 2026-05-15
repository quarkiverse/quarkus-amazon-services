package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.PUBLIC;

import jakarta.inject.Inject;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestBeanClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName asyncClientClassName;
    private final ClassName syncClientClassName;
    private final ClassName beanClassName;
    private final String basePackage;
    private final String quarkusRuntimePackage;
    private final String asyncClientName = "asyncClient";
    private final String syncClientName = "syncClient";

    public TestBeanClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + model.getMetadata().getClientPackageName() + ".runtime";
        this.beanClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "Bean");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(beanClassName)
                .addModifiers(PUBLIC)
                .addAnnotation(ClassName.get("jakarta.enterprise.context", "ApplicationScoped"))
                .addField(
                        FieldSpec.builder(asyncClientClassName, asyncClientName)
                                .addAnnotation(ClassName.get(Inject.class))
                                .build())
                .addField(
                        FieldSpec.builder(syncClientClassName, syncClientName)
                                .addAnnotation(ClassName.get(Inject.class))
                                .build())
                .addMethod(
                        MethodSpec
                                .constructorBuilder()
                                .addModifiers(PUBLIC)
                                .build());

        builder.addMethod(createInvokeMethod(true));
        builder.addMethod(createInvokeMethod(false));
        return builder.build();
    }

    private MethodSpec createInvokeMethod(boolean async) {
        return MethodSpec.methodBuilder("invoke" + (async ? "Async" : "Sync") + "Client")
                .addModifiers(PUBLIC)
                .returns(ClassName.get(String.class))
                .addStatement("return $L.$L.serviceName()", "this", async ? asyncClientName : syncClientName)
                .build();
    }

    @Override
    public ClassName className() {
        return beanClassName;
    }
}
