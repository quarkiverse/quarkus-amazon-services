package io.quarkiverse.amazon.s3.deployment;

import java.util.Collection;
import java.util.HashSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.s3.runtime.S3ClientRoute;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3ClientRoutingProcessor {
    @BuildStep
    io.quarkus.arc.deployment.AdditionalBeanBuildItem qualifier() {
        return new io.quarkus.arc.deployment.AdditionalBeanBuildItem(S3ClientRoute.class);
    }

    private static final DotName CLIENT = DotName.createSimple(S3AsyncClient.class);
    private static final DotName MANAGER = DotName.createSimple("software.amazon.awssdk.transfer.s3.S3TransferManager");
    private static final DotName NAMED = DotName.createSimple(AmazonClient.class);
    private static final DotName CRT = DotName.createSimple(S3Crt.class);

    static AnnotationInstance route(String name, boolean crt) {
        return AnnotationInstance.builder(S3ClientRoute.class).add("value", name).add("crt", crt).build();
    }

    private static AnnotationInstance route(Collection<AnnotationInstance> annotations) {
        String name = annotations.stream().filter(a -> a.name().equals(NAMED)).findFirst()
                .map(a -> a.value().asString()).orElse(ClientUtil.DEFAULT_CLIENT_NAME);
        return route(name, annotations.stream().anyMatch(a -> a.name().equals(CRT)));
    }

    @BuildStep
    InjectionPointTransformerBuildItem injections() {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {
            public boolean appliesTo(Type type) {
                if (type.name().equals(DotNames.INSTANCE) && type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    type = type.asParameterizedType().arguments().get(0);
                }
                return type.name().equals(CLIENT) || type.name().equals(MANAGER);
            }

            public void transform(TransformationContext context) {
                if (context.getQualifiers().stream().noneMatch(a -> a.name().equals(DotNames.ANY))) {
                    context.transform().add(route(context.getQualifiers())).done();
                }
            }
        });
    }

    @BuildStep
    AnnotationsTransformerBuildItem producers(CombinedIndexBuildItem index) {
        var types = new HashSet<DotName>();
        types.add(CLIENT);
        types.add(MANAGER);
        index.getIndex().getAllKnownImplementors(CLIENT).forEach(c -> types.add(c.name()));
        index.getIndex().getAllKnownImplementors(MANAGER).forEach(c -> types.add(c.name()));
        index.getIndex().getAllKnownSubinterfaces(CLIENT).forEach(c -> types.add(c.name()));
        index.getIndex().getAllKnownSubinterfaces(MANAGER).forEach(c -> types.add(c.name()));
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            public void transform(TransformationContext context) {
                DotName type = switch (context.getTarget().kind()) {
                    case CLASS -> context.getTarget().asClass().name();
                    case METHOD -> context.getTarget().asMethod().returnType().name();
                    case FIELD -> context.getTarget().asField().type().name();
                    default -> null;
                };
                if (!types.contains(type)) {
                    return;
                }
                var annotations = context.getAnnotations().stream()
                        .filter(a -> context.getTarget().equals(a.target())).toList();
                if (!context.isClass() && annotations.stream().noneMatch(a -> a.name().equals(DotNames.PRODUCES))) {
                    return;
                }
                boolean hasQualifier = annotations.stream().anyMatch(a -> {
                    var annotation = index.getIndex().getClassByName(a.name());
                    return annotation != null && annotation.hasDeclaredAnnotation(DotNames.QUALIFIER)
                            && !a.name().equals(DotNames.NAMED) && !a.name().equals(DotNames.ANY);
                });
                var transformation = context.transform().add(route(annotations));
                if (!hasQualifier) {
                    transformation.add(jakarta.enterprise.inject.Default.class);
                }
                transformation.done();
            }
        });
    }
}
