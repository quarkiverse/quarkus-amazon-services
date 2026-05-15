package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import jakarta.enterprise.inject.Instance;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestSyntheticBeanClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName asyncClientClassName;
    private final ClassName syncClientClassName;
    private final ClassName syntheticBeanClassName;
    private final String basePackage;
    private final String quarkusRuntimePackage;
    private final String asyncInstanceName = "asyncInstance";
    private final String syncInstanceName = "syncInstance";

    public TestSyntheticBeanClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + model.getMetadata().getClientPackageName() + ".runtime";
        this.syntheticBeanClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "SyntheticBean");
    }

    @Override
    public TypeSpec poetSpec() {
        ParameterizedTypeName asyncInstanceType = ParameterizedTypeName.get(
                ClassName.get(Instance.class),
                asyncClientClassName);
        ParameterizedTypeName syncInstanceType = ParameterizedTypeName.get(
                ClassName.get(Instance.class),
                syncClientClassName);

        TypeSpec.Builder builder = PoetUtils.createClassBuilder(syntheticBeanClassName)
                .addModifiers(PUBLIC)
                .addField(
                        FieldSpec.builder(asyncInstanceType, asyncInstanceName, PRIVATE)
                                .build())
                .addField(
                        FieldSpec.builder(syncInstanceType, syncInstanceName, PRIVATE)
                                .build())
                .addMethod(
                        MethodSpec
                                .constructorBuilder()
                                .addParameter(asyncInstanceType, asyncInstanceName)
                                .addParameter(syncInstanceType, syncInstanceName)
                                .addModifiers(PUBLIC)
                                .addStatement("$L.$L = $L", "this", asyncInstanceName, asyncInstanceName)
                                .addStatement("$L.$L = $L", "this", syncInstanceName, syncInstanceName)
                                .build());

        builder.addMethod(createInvokeMethod(true));
        builder.addMethod(createInvokeMethod(false));
        return builder.build();
    }

    private MethodSpec createInvokeMethod(boolean async) {
        return MethodSpec.methodBuilder("invoke" + (async ? "Async" : "Sync") + "Client")
                .addModifiers(PUBLIC)
                .returns(ClassName.get(String.class))
                .addStatement("return $L.$L.get().serviceName()", "this", async ? asyncInstanceName : syncInstanceName)
                .build();
    }

    @Override
    public ClassName className() {
        return syntheticBeanClassName;
    }
}
