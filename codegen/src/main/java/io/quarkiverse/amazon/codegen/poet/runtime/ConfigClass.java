package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.util.Map;
import java.util.Optional;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class ConfigClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName configClassName;
    private final QuarkusServiceModel quarkusModel;

    private final static ClassName HAS_AMAZON_CLIENT_RUNTIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasAmazonClientRuntimeConfig");
    private final static ClassName ASYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "AsyncHttpClientConfig");
    private final static ClassName SYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "SyncHttpClientConfig");
    private final static ClassName CLIENT_UTIL = ClassName.get("io.quarkiverse.amazon.common.runtime", "ClientUtil");

    public ConfigClass(QuarkusServiceModel quarkusModel) {
        this.model = quarkusModel.intermediateModel();
        this.quarkusModel = quarkusModel;
        this.configClassName = quarkusModel.runtimeClass("Config");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(configClassName)
                .addModifiers(PUBLIC)
                .addSuperinterface(HAS_AMAZON_CLIENT_RUNTIME_CONFIG)
                // @ConfigMapping(prefix = "quarkus.ecr")
                .addAnnotation(AnnotationSpec.builder(ConfigMapping.class)
                        .addMember("prefix", "$S", quarkusModel.configPrefix())
                        .build())
                // @ConfigRoot(phase = ConfigPhase.RUN_TIME)
                .addAnnotation(AnnotationSpec.builder(ConfigRoot.class)
                        .addMember("phase", "$T.RUN_TIME", ConfigPhase.class)
                        .build());

        builder.addMethod(syncClientMethod());
        builder.addMethod(asyncClientMethod());

        if (model.getEndpointOperation().isPresent()) {
            ClassName endpointConfig = configClassName.nestedClass("EndpointDiscoveryConfig");
            builder.addType(TypeSpec.interfaceBuilder("EndpointDiscoveryConfig")
                    .addModifiers(PUBLIC, STATIC)
                    .addAnnotation(ConfigGroup.class)
                    .addMethod(enableEndpointDiscoveryMethod())
                    .build());
            builder.addMethod(MethodSpec.methodBuilder("endpointDiscoveryClients")
                    .addModifiers(PUBLIC, ABSTRACT)
                    .addAnnotation(
                            AnnotationSpec.builder(ConfigDocMapKey.class).addMember("value", "$S", "client-name").build())
                    .addAnnotation(WithParentName.class)
                    .addAnnotation(WithDefaults.class)
                    .addAnnotation(AnnotationSpec.builder(WithUnnamedKey.class)
                            .addMember("value", "$T.DEFAULT_CLIENT_NAME", CLIENT_UTIL).build())
                    .addJavadoc("Endpoint discovery settings. Named clients inherit absent settings from the default client.")
                    .returns(ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), endpointConfig))
                    .build());
            builder.addMethod(MethodSpec.methodBuilder("enableEndpointDiscovery")
                    .addModifiers(PUBLIC, DEFAULT)
                    .returns(ParameterizedTypeName.get(Optional.class, Boolean.class))
                    .addStatement("return enableEndpointDiscovery($T.DEFAULT_CLIENT_NAME)", CLIENT_UTIL)
                    .build());
            builder.addMethod(MethodSpec.methodBuilder("enableEndpointDiscovery")
                    .addModifiers(PUBLIC, DEFAULT)
                    .addParameter(String.class, "clientName")
                    .returns(ParameterizedTypeName.get(Optional.class, Boolean.class))
                    .addStatement("var defaults = endpointDiscoveryClients().get($T.DEFAULT_CLIENT_NAME)", CLIENT_UTIL)
                    .addStatement("var client = endpointDiscoveryClients().getOrDefault(clientName, defaults)")
                    .addStatement("return client.enableEndpointDiscovery().or(defaults::enableEndpointDiscovery)")
                    .build());
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
