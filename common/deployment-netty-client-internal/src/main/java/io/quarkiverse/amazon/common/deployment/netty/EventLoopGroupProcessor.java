package io.quarkiverse.amazon.common.deployment.netty;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.netty.deployment.EventLoopGroupBuildItem;

public class EventLoopGroupProcessor {

    @BuildStep
    io.quarkiverse.amazon.common.deployment.netty.EventLoopGroupBuildItem convert(EventLoopGroupBuildItem nettyEventLoopGroup) {
        return new io.quarkiverse.amazon.common.deployment.netty.EventLoopGroupBuildItem(
                nettyEventLoopGroup.getBossEventLoopGroup(),
                nettyEventLoopGroup.getMainEventLoopGroup());
    }
}
