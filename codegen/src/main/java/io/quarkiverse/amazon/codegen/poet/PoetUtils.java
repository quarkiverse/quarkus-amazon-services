package io.quarkiverse.amazon.codegen.poet;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import software.amazon.awssdk.annotations.Generated;

public final class PoetUtils {
    private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", "io.quarkiverse.amazon:codegen")
            .build();

    public static AnnotationSpec generatedAnnotation() {
        return GENERATED;
    }

    private PoetUtils() {
    }

    public static TypeSpec.Builder createClassBuilder(ClassName name) {
        return TypeSpec.classBuilder(name).addAnnotation(PoetUtils.generatedAnnotation());
    }

    public static TypeSpec.Builder createInterfaceBuilder(ClassName name) {
        return TypeSpec.interfaceBuilder(name).addAnnotation(PoetUtils.generatedAnnotation());
    }
}
