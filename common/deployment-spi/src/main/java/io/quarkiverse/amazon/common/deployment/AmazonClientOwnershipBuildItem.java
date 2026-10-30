package io.quarkiverse.amazon.common.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/** Declares a standard client pair managed by a dedicated extension, not by the generic SDK extension. */
public final class AmazonClientOwnershipBuildItem extends MultiBuildItem {
    private final Set<DotName> clientTypes;

    public AmazonClientOwnershipBuildItem(DotName syncClient, DotName asyncClient) {
        clientTypes = Set.of(syncClient, asyncClient);
    }

    public Set<DotName> getClientTypes() {
        return clientTypes;
    }
}
