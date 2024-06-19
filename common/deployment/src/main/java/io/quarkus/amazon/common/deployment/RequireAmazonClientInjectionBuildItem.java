package io.quarkus.amazon.common.deployment;

import java.util.Collection;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes what client names are required.
 *
 */
public final class RequireAmazonClientInjectionBuildItem extends MultiBuildItem {
    private final DotName className;
    private final Collection<String> names;

    public RequireAmazonClientInjectionBuildItem(DotName className, Collection<String> names) {
        this.className = className;
        this.names = names;
    }

    public DotName getClassName() {
        return className;
    }

    public Collection<String> getNames() {
        return names;
    }
}
