package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import java.util.Optional;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class ConfigClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName configClassName;
    private final String clientPackageName;
    private final String quarkusRuntimePackage;

    private final static ClassName HAS_AMAZON_CLIENT_RUNTIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasAmazonClientRuntimeConfig");
    private final static ClassName ASYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "AsyncHttpClientConfig");
    private final static ClassName SYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "SyncHttpClientConfig");

    public ConfigClass(IntermediateModel model) {
        this.model = model;
        this.clientPackageName = model.getMetadata().getClientPackageName();
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + model.getMetadata().getClientPackageName() + ".runtime";
        this.configClassName = ClassName.get(quarkusRuntimePackage, model.getMetadata().getServiceName() + "Config");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(configClassName)
                .addModifiers(PUBLIC)
                .addSuperinterface(HAS_AMAZON_CLIENT_RUNTIME_CONFIG)
                // @ConfigMapping(prefix = "quarkus.ecr")
                .addAnnotation(AnnotationSpec.builder(ConfigMapping.class)
                        .addMember("prefix", "$S", "quarkus." + clientPackageName)
                        .build())
                // @ConfigRoot(phase = ConfigPhase.RUN_TIME)
                .addAnnotation(AnnotationSpec.builder(ConfigRoot.class)
                        .addMember("phase", "$T.RUN_TIME", ConfigPhase.class)
                        .build());

        builder.addMethod(syncClientMethod());
        builder.addMethod(asyncClientMethod());

        if (model.getEndpointOperation().isPresent()) {
            builder.addMethod(enableEndpointDiscoveryMethod());
        }

        return builder.build();
    }

    // /**
    // * Sync HTTP transport configurations
    // */
    // @ConfigDocSection
    // SyncHttpClientConfig syncClient();
    private MethodSpec syncClientMethod() {
        return MethodSpec.methodBuilder("syncClient")
                .addModifiers(ABSTRACT, PUBLIC)
                .addAnnotation(ConfigDocSection.class)
                .addJavadoc("Sync HTTP transport configurations")
                .returns(SYNC_HTTP_CLIENT_CONFIG)
                .build();
    }

    // /**
    // * Async HTTP transport configurations
    // */
    // @ConfigDocSection
    // AsyncHttpClientConfig asyncClient();
    private MethodSpec asyncClientMethod() {
        return MethodSpec.methodBuilder("asyncClient")
                .addModifiers(ABSTRACT, PUBLIC)
                .addAnnotation(ConfigDocSection.class)
                .addJavadoc("Async HTTP transport configurations")
                .returns(ASYNC_HTTP_CLIENT_CONFIG)
                .build();
    }

    // /**
    //  * Enable service endpoint discovery.
    //  */
    // @ConfigDocSection("true for services that requires endpoint discovery.")
    // Optional<Boolean> enableEndpointDiscovery();
    private MethodSpec enableEndpointDiscoveryMethod() {
        return MethodSpec.methodBuilder("enableEndpointDiscovery")
                .addModifiers(ABSTRACT, PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ConfigDocDefault.class)
                        .addMember("value", "$S", "true for services that requires endpoint discovery.")
                        .build())
                .addJavadoc("Enable service endpoint discovery.")
                .returns(ParameterizedTypeName.get(Optional.class, Boolean.class))
                .build();
    }

    @Override
    public ClassName className() {
        return configClassName;
    }
}
