package io.quarkiverse.amazon.codegen.poet.devservices;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class DevServicesProcessorClass implements ClassSpec {
    private final ClassName processorClassName;
    private final ClassName buildTimeConfigClassName;
    private final String classPrefix;
    private final String configName;

    private final static ClassName ABSTRACT_DEV_SERVICES_AWS_STACK_PROCESSOR = ClassName.get(
            "io.quarkiverse.amazon.common.deployment.spi",
            "AbstractDevServicesAwsStackProcessor");
    private final static ClassName DEV_SERVICES_AWS_STACK_PROVIDER_BUILD_ITEM = ClassName.get(
            "io.quarkiverse.amazon.common.deployment.spi",
            "DevServicesAwsStackProviderBuildItem");
    private final static ClassName GLOBAL_DEV_SERVICES_BUILD_TIME_CONFIG = ClassName.get(
            "io.quarkiverse.amazon.common.runtime",
            "GlobalDevServicesBuildTimeConfig");
    private final static ClassName BUILD_STEP = ClassName.get("io.quarkus.deployment.annotations", "BuildStep");

    public DevServicesProcessorClass(QuarkusServiceModel quarkusModel) {
        this.processorClassName = quarkusModel.devServicesClass("DevServicesAwsStackProcessor");
        this.buildTimeConfigClassName = quarkusModel.runtimeClass("BuildTimeConfig");
        this.classPrefix = quarkusModel.classPrefix();
        this.configName = quarkusModel.configName();
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(processorClassName)
                .addModifiers(PUBLIC)
                .superclass(ABSTRACT_DEV_SERVICES_AWS_STACK_PROCESSOR)
                .addMethod(setupMethod())
                .build();
    }

    // @BuildStep
    // DevServicesAwsStackProviderBuildItem setupEcr(EcrBuildTimeConfig clientBuildTimeConfig,
    // GlobalDevServicesBuildTimeConfig globalConfig) {
    // return this.setup("ecr", clientBuildTimeConfig.devservices(), globalConfig);
    // }
    private MethodSpec setupMethod() {
        return MethodSpec.methodBuilder("setup" + classPrefix)
                .addAnnotation(BUILD_STEP)
                .returns(DEV_SERVICES_AWS_STACK_PROVIDER_BUILD_ITEM)
                .addParameter(buildTimeConfigClassName, "clientBuildTimeConfig")
                .addParameter(GLOBAL_DEV_SERVICES_BUILD_TIME_CONFIG, "globalConfig")
                .addStatement("return this.setup($S, clientBuildTimeConfig.devservices(), globalConfig)", configName)
                .build();
    }

    @Override
    public ClassName className() {
        return processorClassName;
    }
}
