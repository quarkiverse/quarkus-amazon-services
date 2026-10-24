package io.quarkiverse.amazon.common.deployment;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Produced once the HTTP clients required by the configuration are known to be on the classpath.
 * The steps building the transports and the clients wait for it, so that a missing HTTP client
 * is reported as such instead of failing later on a missing bean or class.
 */
public final class AmazonHttpClientsCheckedBuildItem extends EmptyBuildItem {
}
