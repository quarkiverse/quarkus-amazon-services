package io.quarkus.amazon.common.deployment.netty;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.netty.deployment.EventLoopGroupBuildItem;

public class EventLoopGroupProcessor {

    @BuildStep
    io.quarkus.amazon.common.deployment.spi.EventLoopGroupBuildItem convert(EventLoopGroupBuildItem nettyEventLoopGroup) {
        return new io.quarkus.amazon.common.deployment.spi.EventLoopGroupBuildItem(nettyEventLoopGroup.getBossEventLoopGroup(),
                nettyEventLoopGroup.getMainEventLoopGroup());
    }
}
