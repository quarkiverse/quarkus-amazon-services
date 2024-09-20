package io.quarkus.amazon.common.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes what client names are required.
 *
 */
public final class RequireAmazonClientInjectionBuildItem extends MultiBuildItem {
    private final DotName className;
    private final String name;

    public RequireAmazonClientInjectionBuildItem(DotName className, String name) {
        this.className = className;
        this.name = name;
    }

    public DotName getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }
}
