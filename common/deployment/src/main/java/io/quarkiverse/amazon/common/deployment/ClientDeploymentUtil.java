package io.quarkiverse.amazon.common.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;

public final class ClientDeploymentUtil {

    private static final DotName AWS_CLIENT_NAME = DotName
            .createSimple(io.quarkiverse.amazon.common.AmazonClient.class.getName());
    private static final DotName AWS_CLIENT_BUILDER_NAME = DotName
            .createSimple(io.quarkiverse.amazon.common.AmazonClientBuilder.class.getName());
    private static final AnnotationInstance[] EMPTY_ANNOTATIONS = new AnnotationInstance[0];

    private ClientDeploymentUtil() {
    }

    public static String getNamedClientInjection(InjectionPointInfo injectionPoint) {
        var named = injectionPoint.getRequiredQualifier(AWS_CLIENT_NAME);
        if (named != null) {
            return named.value("value").asString();
        } else {
            return ClientUtil.DEFAULT_CLIENT_NAME;
        }
    }

    public static ExtendedBeanConfigurator namedClient(ExtendedBeanConfigurator beanConfigurator, String clientName) {
        return named(beanConfigurator, AWS_CLIENT_NAME, clientName);
    }

    public static ExtendedBeanConfigurator namedBuilder(ExtendedBeanConfigurator beanConfigurator, String clientName) {
        return named(beanConfigurator, AWS_CLIENT_BUILDER_NAME, clientName);
    }

    public static AnnotationInstance[] injectionPointAnnotationsClient(String clientName) {
        return injectionPointAnnotations(AWS_CLIENT_NAME, clientName);
    }

    public static AnnotationInstance[] injectionPointAnnotationsBuilder(String clientName) {
        return injectionPointAnnotations(AWS_CLIENT_BUILDER_NAME, clientName);
    }

    private static ExtendedBeanConfigurator named(ExtendedBeanConfigurator beanConfigurator, DotName annotationName,
            String clientName) {
        if (ClientUtil.isDefaultClient(clientName)) {
            beanConfigurator.addQualifier(DotNames.DEFAULT);
        } else {
            beanConfigurator.addQualifier().annotation(annotationName).addValue("value", clientName).done();
        }

        return beanConfigurator;
    }

    private static AnnotationInstance[] injectionPointAnnotations(DotName annotationName, String clientName) {
        if (ClientUtil.isDefaultClient(clientName)) {
            return EMPTY_ANNOTATIONS;
        } else {
            return new AnnotationInstance[] {
                    AnnotationInstance.builder(annotationName).add("value", clientName).build()
            };
        }
    }
}
