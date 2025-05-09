package io.quarkiverse.amazon.codegen.poet.deployment;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.util.List;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class ProcessorClass implements ClassSpec {
    private final ClassName syncClientClassName;
    private final ClassName syncClientBuilderClassName;
    private final ClassName asyncClientClassName;
    private final ClassName asyncClientBuilderClassName;
    private final ClassName processorClassName;
    private final ClassName buildTimeConfigClassName;
    private final String clientPackageName;
    private final ClassName recorderClassName;
    private final String basePackage;
    private final String quarkusDeploymentPackage;
    private final String quarkusRuntimePackage;
    private final String quarkusFeatureName;

    private final static TypeName ABSTRACT_AMAZON_SERVICE_PROCESSOR = ClassName.get(
            "io.quarkiverse.amazon.common.deployment",
            "AbstractAmazonServiceProcessor");
    private final static TypeName CLASS_OF_UNKNOWN_TYPE = ParameterizedTypeName.get(ClassName.get(Class.class),
            WildcardTypeName.subtypeOf(ClassName.OBJECT));
    private final static TypeName DOTNAME = ClassName.get("org.jboss.jandex", "DotName");
    private final static TypeName HAS_TRANSPORT_BUILD_TIME_CONFIG = ClassName.get(
            "io.quarkiverse.amazon.common.runtime",
            "HasTransportBuildTimeConfig");
    private final static TypeName HAS_SDK_BUILD_TIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasSdkBuildTimeConfig");
    private final static TypeName EXECUTION_TIME = ClassName.get("io.quarkus.deployment.annotations", "ExecutionTime");
    private final static ClassName BUILD_STEP = ClassName.get("io.quarkus.deployment.annotations", "BuildStep");
    private final static ClassName RECORD = ClassName.get("io.quarkus.deployment.annotations", "Record");
    private final static ClassName BUILD_PRODUCER = ClassName.get("io.quarkus.deployment.annotations", "BuildProducer");
    private final static TypeName REQUIRE_AMAZON_CLIENT_INJECTION_BUILD_ITEM = ClassName
            .get("io.quarkiverse.amazon.common.deployment", "RequireAmazonClientInjectionBuildItem");
    private final static TypeName AMAZON_CLIENT_EXTENSION_BUILDER_INSTANCE_BUILD_ITEM = ClassName
            .get("io.quarkiverse.amazon.common.deployment", "AmazonClientExtensionBuilderInstanceBuildItem");
    private final static TypeName AMAZON_CLIENT_EXTENSION_BUILD_ITEM = ClassName.get(
            "io.quarkiverse.amazon.common.deployment",
            "AmazonClientExtensionBuildItem");

    public ProcessorClass(IntermediateModel model) {
        this.basePackage = model.getMetadata().getFullClientPackageName();

        this.clientPackageName = model.getMetadata().getClientPackageName();
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.syncClientBuilderClassName = ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface());
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.asyncClientBuilderClassName = ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface());

        this.quarkusDeploymentPackage = "io.quarkiverse.amazon." + this.clientPackageName + ".deployment";
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + this.clientPackageName + ".runtime";
        this.quarkusFeatureName = "amazon-sdk-" + this.clientPackageName;

        this.processorClassName = ClassName.get(this.quarkusDeploymentPackage,
                model.getMetadata().getServiceName() + "Processor");
        this.buildTimeConfigClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "BuildTimeConfig");
        this.recorderClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "Recorder");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(processorClassName)
                .addModifiers(PUBLIC)
                .superclass(ABSTRACT_AMAZON_SERVICE_PROCESSOR)
                .addField(FieldSpec
                        .builder(buildTimeConfigClassName, "buildTimeConfig")
                        .build())
                .addField(FieldSpec
                        .builder(String.class, "AMAZON_CLIENT_NAME")
                        .addModifiers(PRIVATE, STATIC, FINAL)
                        .initializer("$S", this.quarkusFeatureName)
                        .build());
        builder.addMethod(amazonServiceClientNameMethod());
        builder.addMethod(configNameMethod());
        builder.addMethod(syncClientNameMethod());
        builder.addMethod(syncClientBuilderClassMethod());
        builder.addMethod(asyncClientNameMethod());
        builder.addMethod(asyncClientBuilderClassMethod());
        builder.addMethod(builtinInterceptorsPathMethod());
        builder.addMethod(transportBuildTimeConfigMethod());
        // sdkBuildTimeConfig
        builder.addMethod(sdkBuildTimeConfigMethod());
        // createBuilders
        builder.addMethod(createBuildersMethod());
        // setup
        builder.addMethod(setupMethod());

        return builder.build();
    }

    private MethodSpec amazonServiceClientNameMethod() {
        return MethodSpec.methodBuilder("amazonServiceClientName")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(ClassName.get(String.class))
                .addStatement("return AMAZON_CLIENT_NAME")
                .build();
    }

    private MethodSpec configNameMethod() {
        return MethodSpec.methodBuilder("configName")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(ClassName.get(String.class))
                .addStatement("return $S", clientPackageName)
                .build();
    }

    private MethodSpec syncClientNameMethod() {
        return MethodSpec.methodBuilder("syncClientName")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(DOTNAME)
                .addStatement("return $T.createSimple($T.class.getName())",
                        DOTNAME,
                        syncClientClassName)
                .build();
    }

    private MethodSpec syncClientBuilderClassMethod() {
        return MethodSpec.methodBuilder("syncClientBuilderClass")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(CLASS_OF_UNKNOWN_TYPE)
                .addStatement("return $T.class", syncClientBuilderClassName)
                .build();
    }

    private MethodSpec asyncClientNameMethod() {
        return MethodSpec.methodBuilder("asyncClientName")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(DOTNAME)
                .addStatement("return $T.createSimple($T.class.getName())",
                        DOTNAME,
                        asyncClientClassName)
                .build();
    }

    private MethodSpec asyncClientBuilderClassMethod() {
        return MethodSpec.methodBuilder("asyncClientBuilderClass")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(CLASS_OF_UNKNOWN_TYPE)
                .addStatement("return $T.class", asyncClientBuilderClassName)
                .build();
    }

    private MethodSpec builtinInterceptorsPathMethod() {
        return MethodSpec.methodBuilder("builtinInterceptorsPath")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(ClassName.get(String.class))
                .addStatement("return $S",
                        "software/amazon/awssdk/services/" + clientPackageName + "/execution.interceptors")
                .build();
    }

    private MethodSpec transportBuildTimeConfigMethod() {
        return MethodSpec.methodBuilder("transportBuildTimeConfig")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(HAS_TRANSPORT_BUILD_TIME_CONFIG)
                .addStatement("return buildTimeConfig")
                .build();
    }

    private MethodSpec sdkBuildTimeConfigMethod() {
        return MethodSpec.methodBuilder("sdkBuildTimeConfig")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(HAS_SDK_BUILD_TIME_CONFIG)
                .addStatement("return buildTimeConfig")
                .build();
    }

    // @BuildStep
    // @Record(ExecutionTime.RUNTIME_INIT)
    // void createBuilders(
    // EcrRecorder recorder,
    // List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
    // BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem>
    // builderInstances) {
    // createExtensionBuilders(recorder, amazonClientInjections, builderInstances);
    // }
    private MethodSpec createBuildersMethod() {
        return MethodSpec.methodBuilder("createBuilders")
                .addAnnotation(BUILD_STEP)
                .addAnnotation(AnnotationSpec.builder(RECORD)
                        .addMember("value", "$T.RUNTIME_INIT",
                                EXECUTION_TIME)
                        .build())
                .addParameter(recorderClassName, "recorder")
                .addParameter(
                        ParameterizedTypeName.get(ClassName.get(List.class),
                                REQUIRE_AMAZON_CLIENT_INJECTION_BUILD_ITEM),
                        "amazonClientInjections")
                .addParameter(
                        ParameterizedTypeName.get(BUILD_PRODUCER, AMAZON_CLIENT_EXTENSION_BUILDER_INSTANCE_BUILD_ITEM),
                        "builderInstances")
                .addStatement("createExtensionBuilders(recorder, amazonClientInjections, builderInstances)")
                .build();
    }

    // @BuildStep
    // @Record(ExecutionTime.RUNTIME_INIT)
    // void setup(
    // EcrRecorder recorder,
    // BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

    // setupExtension(recorder, amazonExtensions);
    // }
    private MethodSpec setupMethod() {
        return MethodSpec.methodBuilder("setup")
                .addAnnotation(BUILD_STEP)
                .addAnnotation(AnnotationSpec.builder(RECORD)
                        .addMember("value", "$T.RUNTIME_INIT",
                                EXECUTION_TIME)
                        .build())
                .addParameter(recorderClassName, "recorder")
                .addParameter(ParameterizedTypeName.get(BUILD_PRODUCER, AMAZON_CLIENT_EXTENSION_BUILD_ITEM),
                        "amazonExtensions")
                .addStatement("setupExtension(recorder, amazonExtensions)")
                .build();
    }

    @Override
    public ClassName className() {
        return processorClassName;
    }
}
