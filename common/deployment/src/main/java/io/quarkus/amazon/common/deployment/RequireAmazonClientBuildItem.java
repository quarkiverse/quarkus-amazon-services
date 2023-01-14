package io.quarkus.amazon.common.deployment;

import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes what clients are required by a given extension.
 *
 * An extension that would want to wrap a client can request it and listen
 * to ProvideAmazonClientAsyncBuildItem or ProvideAmazonClientSyncBuildItem
 * to be sure the client is correctly configured
 */
public final class RequireAmazonClientBuildItem extends MultiBuildItem {
    private final Optional<DotName> syncClassName;
    private final Optional<DotName> asyncClassName;

    public RequireAmazonClientBuildItem(Optional<DotName> syncClassName, Optional<DotName> asyncClassName) {
        this.syncClassName = syncClassName;
        this.asyncClassName = asyncClassName;
    }

    public Optional<DotName> getSyncClassName() {
        return syncClassName;
    }

    public Optional<DotName> getAsyncClassName() {
        return asyncClassName;
    }
}
