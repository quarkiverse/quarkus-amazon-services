package io.quarkiverse.amazon.codegen.poet.runtime;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import io.quarkiverse.amazon.codegen.poet.PoetUtils;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class RecorderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName syncClientClassName;
    private final ClassName asyncClientClassName;
    private final ClassName recorderClassName;
    private final ClassName configClassName;
    private final String basePackage;
    private final String quarkusRuntimePackage;

    private final static ClassName AMAZON_CLIENT_RUNTIME_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "HasAmazonClientRuntimeConfig");
    private final static ClassName AMAZON_CLIENT_RECORDER = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "AmazonClientRecorder");
    private final static ClassName ASYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "AsyncHttpClientConfig");
    private final static ClassName SYNC_HTTP_CLIENT_CONFIG = ClassName.get("io.quarkiverse.amazon.common.runtime",
            "SyncHttpClientConfig");

    public RecorderClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.syncClientClassName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.asyncClientClassName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.quarkusRuntimePackage = "io.quarkiverse.amazon." + model.getMetadata().getClientPackageName() + ".runtime";
        this.recorderClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "Recorder");
        this.configClassName = ClassName.get(this.quarkusRuntimePackage,
                model.getMetadata().getServiceName() + "Config");
    }

    @Override
    public TypeSpec poetSpec() {
        ParameterizedTypeName runtimeConfig = ParameterizedTypeName.get(ClassName.get(RuntimeValue.class), configClassName);
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(recorderClassName)
                .addModifiers(PUBLIC)
                .addAnnotation(Recorder.class)
                .superclass(AMAZON_CLIENT_RECORDER)
                .addField(FieldSpec
                        .builder(runtimeConfig, "config")
                        .addModifiers(FINAL).build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC)
                        .addParameter(runtimeConfig, "config")
                        .addStatement("this.config = config")
                        .build());

        builder.addMethod(getAmazonClientsConfigMethod());
        builder.addMethod(getAsyncClientConfigMethod());
        builder.addMethod(getSyncClientConfigMethod());
        builder.addMethod(getSyncClientBuilderMethod());
        builder.addMethod(getAsyncClientBuilderMethod());

        return builder.build();
    }

    private MethodSpec getAmazonClientsConfigMethod() {
        return MethodSpec.methodBuilder("getAmazonClientsConfig")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(RuntimeValue.class),
                        WildcardTypeName.subtypeOf(AMAZON_CLIENT_RUNTIME_CONFIG)))
                .addStatement("return config")
                .build();
    }

    private MethodSpec getAsyncClientConfigMethod() {
        return MethodSpec.methodBuilder("getAsyncClientConfig")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(ASYNC_HTTP_CLIENT_CONFIG)
                .addStatement("return config.getValue().asyncClient()")
                .build();
    }

    private MethodSpec getSyncClientConfigMethod() {
        return MethodSpec.methodBuilder("getSyncClientConfig")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(SYNC_HTTP_CLIENT_CONFIG)
                .addStatement("return config.getValue().syncClient()")
                .build();
    }

    private MethodSpec getSyncClientBuilderMethod() {
        Builder builder = MethodSpec.methodBuilder("getSyncClientBuilder")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(AwsSyncClientBuilder.class),
                        WildcardTypeName.subtypeOf(Object.class), WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("var builder = $T.builder()", syncClientClassName);

        if (model.getEndpointOperation().isPresent()) {
            builder.addStatement(
                    "config.getValue().enableEndpointDiscovery().ifPresent(enableEndpointDiscovery -> builder.endpointDiscoveryEnabled(enableEndpointDiscovery))");
        }

        return builder
                .addStatement("return builder")
                .build();
    }

    private MethodSpec getAsyncClientBuilderMethod() {
        Builder builder = MethodSpec.methodBuilder("getAsyncClientBuilder")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(AwsAsyncClientBuilder.class),
                        WildcardTypeName.subtypeOf(Object.class), WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("var builder = $T.builder()", asyncClientClassName);

        if (model.getEndpointOperation().isPresent()) {
            builder.addStatement(
                    "config.getValue().enableEndpointDiscovery().ifPresent(enableEndpointDiscovery -> builder.endpointDiscoveryEnabled(enableEndpointDiscovery))");
        }

        return builder
                .addStatement("return builder")
                .build();
    }

    @Override
    public ClassName className() {
        return recorderClassName;
    }
}
