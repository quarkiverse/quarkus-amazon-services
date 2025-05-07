package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class BuildTimeConfigClass implements ClassSpec {
    private final ClassName buildTimeConfigClassName;
    private final String clientPackageName;
    private final String up;
    private final String quarkusRuntimePackage;

    private final static TypeName HAS_SDK_BUILD_TIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasSdkBuildTimeConfig");
    private final static ClassName HAS_TRANSPORT_BUILD_TIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasTransportBuildTimeConfig");
    private final static ClassName DEV_SERVICES_BUILD_TIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "DevServicesBuildTimeConfig");

    public BuildTimeConfigClass(IntermediateModel model) {
        this.clientPackageName = model.getMetadata().getClientPackageName();
        this.up = model.getMetadata().getDescriptiveServiceName();
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + this.clientPackageName + ".runtime";
        this.buildTimeConfigClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "BuildTimeConfig");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(buildTimeConfigClassName)
                .addModifiers(PUBLIC)
                .addSuperinterface(HAS_SDK_BUILD_TIME_CONFIG)
                .addSuperinterface(HAS_TRANSPORT_BUILD_TIME_CONFIG)
                // Amazon ECR build time configuration
                .addJavadoc("$L build time configuration", up)
                // @ConfigMapping(prefix = "quarkus.ecr")
                .addAnnotation(AnnotationSpec.builder(ConfigMapping.class)
                        .addMember("prefix", "$S", "quarkus." + clientPackageName)
                        .build())
                // @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
                .addAnnotation(AnnotationSpec.builder(ConfigRoot.class)
                        .addMember("phase", "$T.BUILD_AND_RUN_TIME_FIXED", ConfigPhase.class)
                        .build());

        builder.addMethod(devservicesMethod());

        return builder.build();
    }

    // /**
    // * Config for dev services
    // */
    // DevServicesBuildTimeConfig devservices();
    private MethodSpec devservicesMethod() {
        return MethodSpec.methodBuilder("devservices")
                .addModifiers(ABSTRACT, PUBLIC)
                .addJavadoc("Config for dev services")
                .returns(DEV_SERVICES_BUILD_TIME_CONFIG)
                .build();
    }

    @Override
    public ClassName className() {
        return buildTimeConfigClassName;
    }
}
