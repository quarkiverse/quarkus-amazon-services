package io.quarkiverse.amazon.codegen.poet.deployment;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class TestProcessorClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName asyncClientClassName;
    private final ClassName syncClientClassName;
    private final ClassName processorClassName;
    private final ClassName recorderClassName;
    private final String basePackage;
    private final String quarkusBasePackage;
    private final String quarkusDeploymentPackage;
    private final String quarkusRuntimePackage;

    private final ClassName buildProducerClassName = ClassName.get("io.quarkus.deployment.annotations", "BuildProducer");
    private final ClassName buildStepClassName = ClassName.get("io.quarkus.deployment.annotations", "BuildStep");
    private final ClassName clientUtilClassName = ClassName.get("io.quarkiverse.amazon.common.runtime", "ClientUtil");
    private final ClassName dotNameClassName = ClassName.get("org.jboss.jandex", "DotName");
    private final ClassName executionTimeClassName = ClassName.get("io.quarkus.deployment.annotations", "ExecutionTime");
    private final ClassName recordClassName = ClassName.get("io.quarkus.deployment.annotations", "Record");
    private final ClassName requireAmazonClientInjectionBuildItemClassName = ClassName
            .get("io.quarkiverse.amazon.common.deployment", "RequireAmazonClientInjectionBuildItem");
    private final ClassName syntheticBeanBuildItemClassName = ClassName.get("io.quarkus.arc.deployment",
            "SyntheticBeanBuildItem");

    public TestProcessorClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.quarkusBasePackage = "io.quarkiverse.amazon." + model.getMetadata().getClientPackageName();
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.quarkusDeploymentPackage = quarkusBasePackage + ".deployment";
        this.quarkusRuntimePackage = quarkusBasePackage + ".runtime";
        this.processorClassName = ClassName.get(this.quarkusDeploymentPackage,
                model.getMetadata().getServiceName() + "TestProcessor");
        this.recorderClassName = ClassName.get(this.quarkusBasePackage,
                model.getMetadata().getServiceName() + "TestRecorder");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(processorClassName)
                .addModifiers(PUBLIC)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC).build());

        builder.addMethod(addSyntheticBeanBuildStep());
        return builder.build();
    }

    private MethodSpec addSyntheticBeanBuildStep() {
        String recorderName = "recorder";
        String syntheticBeanProducerName = "syntheticBeanProducer";
        String requireAmazonClientInjectionProducerName = "requireAmazonClientInjectionProducer";
        String builderName = "builder";
        String asyncClientClassNameName = "asyncClientClassName";
        String syncClientClassNameName = "syncClientClassName";
        ClassName syntheticBeanName = ClassName.get(quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "SyntheticBean");

        var asyncIp = CodeBlock.builder()
                .add(
                        "$T.create($T.INSTANCE, $T.create($L))",
                        ClassName.get("org.jboss.jandex", "ParameterizedType"),
                        ClassName.get("io.quarkus.arc.processor", "DotNames"),
                        ClassName.get("org.jboss.jandex", "ClassType"),
                        asyncClientClassNameName)
                .build();
        var syncIp = CodeBlock.builder()
                .add(
                        "$T.create($T.INSTANCE, $T.create($L))",
                        ClassName.get("org.jboss.jandex", "ParameterizedType"),
                        ClassName.get("io.quarkus.arc.processor", "DotNames"),
                        ClassName.get("org.jboss.jandex", "ClassType"),
                        syncClientClassNameName)
                .build();

        return MethodSpec.methodBuilder("registerSyntheticBean")
                .addModifiers(PUBLIC)
                .addAnnotation(
                        AnnotationSpec.builder(recordClassName)
                                .addMember("value", "$T.STATIC_INIT", executionTimeClassName)
                                .build())
                .addAnnotation(buildStepClassName)
                .addParameter(recorderClassName, recorderName)
                .addParameter(ParameterizedTypeName.get(buildProducerClassName, syntheticBeanBuildItemClassName),
                        syntheticBeanProducerName)
                .addParameter(ParameterizedTypeName.get(buildProducerClassName, requireAmazonClientInjectionBuildItemClassName),
                        requireAmazonClientInjectionProducerName)
                .addStatement("var $N = $T.createSimple($T.class)", asyncClientClassNameName, dotNameClassName,
                        asyncClientClassName)
                .addStatement("var $N = $T.createSimple($T.class)", syncClientClassNameName, dotNameClassName,
                        syncClientClassName)
                .addStatement("var $N = $T.configure($T.class)", builderName, syntheticBeanBuildItemClassName,
                        syntheticBeanName)
                .addStatement("$N.scope($L.class)", builderName, ClassName.get("jakarta.inject", "Singleton"))
                .addStatement("$N.unremovable()", builderName)
                .addCode("$N.addInjectionPoint($L)", builderName, asyncIp).addStatement("")
                .addCode("$N.addInjectionPoint($L)", builderName, syncIp).addStatement("")
                .addStatement("$N.createWith($N.createSyntheticBean())", builderName, recorderName)
                .addStatement("$N.produce($N.done())", syntheticBeanProducerName, builderName)
                .addStatement(
                        "$N.produce(new $T($N, $T.DEFAULT_CLIENT_NAME))",
                        requireAmazonClientInjectionProducerName,
                        requireAmazonClientInjectionBuildItemClassName,
                        asyncClientClassNameName,
                        clientUtilClassName)
                .addStatement(
                        "$N.produce(new $T($N, $T.DEFAULT_CLIENT_NAME))",
                        requireAmazonClientInjectionProducerName,
                        requireAmazonClientInjectionBuildItemClassName,
                        syncClientClassNameName,
                        clientUtilClassName)
                .build();
    }

    @Override
    public ClassName className() {
        return processorClassName;
    }
}
