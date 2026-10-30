package io.quarkiverse.amazon.sdk.deployment;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

final class ClientBuilderFactoryGenerator {
    static String generate(DotName client, DotName builder, BuildProducer<GeneratedClassBuildItem> classes) {
        String factory = "io.quarkiverse.amazon.sdk.runtime.generated." + client + "BuilderFactory";
        try (ClassCreator creator = ClassCreator.builder()
                .className(factory)
                .interfaces(Supplier.class)
                .classOutput(new GeneratedClassGizmoAdaptor(classes, true))
                .build()) {
            MethodCreator get = creator.getMethodCreator("get", Object.class).setModifiers(Modifier.PUBLIC);
            get.returnValue(get.invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(client.toString(), "builder",
                    builder.toString())));
        }
        return factory;
    }
}
