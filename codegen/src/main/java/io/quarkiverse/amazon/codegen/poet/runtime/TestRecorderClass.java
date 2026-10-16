package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.PUBLIC;

import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestRecorderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName asyncClientClassName;
    private final ClassName syncClientClassName;
    private final ClassName recorderClassName;
    private final ClassName syntheticBeanClassName;
    private final String basePackage;

    public TestRecorderClass(QuarkusServiceModel quarkusModel) {
        this.model = quarkusModel.intermediateModel();
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.recorderClassName = quarkusModel.baseClass("TestRecorder");
        this.syntheticBeanClassName = quarkusModel.runtimeClass("SyntheticBean");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(recorderClassName)
                .addModifiers(PUBLIC)
                .addAnnotation(Recorder.class)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC).build());

        builder.addMethod(getSyntheticBeanMethod());
        return builder.build();
    }

    private MethodSpec getSyntheticBeanMethod() {
        var syntheticBeanClass = syntheticBeanClassName;

        ParameterizedTypeName returnType = ParameterizedTypeName.get(
                ClassName.get(Function.class),
                ParameterizedTypeName.get(
                        ClassName.get("io.quarkus.arc", "SyntheticCreationalContext"),
                        syntheticBeanClass),
                syntheticBeanClass);
        ParameterizedTypeName asyncInstanceType = ParameterizedTypeName.get(
                ClassName.get(Instance.class),
                asyncClientClassName);
        ParameterizedTypeName syncInstanceType = ParameterizedTypeName.get(
                ClassName.get(Instance.class),
                syncClientClassName);

        String asyncRefName = "asyncRef";
        String syncRefName = "syncRef";
        String contextName = "context";

        var lambda = CodeBlock.builder()
                .beginControlFlow("return $N ->", contextName)
                .addStatement("$T $N = $N.getInjectedReference(new $T() {})", asyncInstanceType, asyncRefName, contextName,
                        ParameterizedTypeName.get(ClassName.get(TypeLiteral.class), asyncInstanceType))
                .addStatement("$T $N = $N.getInjectedReference(new $T() {})", syncInstanceType, syncRefName, contextName,
                        ParameterizedTypeName.get(ClassName.get(TypeLiteral.class), syncInstanceType))
                .addStatement("return new $T($L, $L)", syntheticBeanClass, asyncRefName, syncRefName)
                .endControlFlow("")
                .build();

        return MethodSpec.methodBuilder("createSyntheticBean")
                .addModifiers(PUBLIC)
                .returns(returnType)
                .addCode(lambda)
                .build();
    }

    @Override
    public ClassName className() {
        return recorderClassName;
    }
}
