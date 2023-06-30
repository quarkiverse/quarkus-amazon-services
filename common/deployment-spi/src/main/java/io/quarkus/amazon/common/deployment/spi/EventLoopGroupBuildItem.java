package io.quarkus.amazon.common.deployment.spi;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkus.builder.item.SimpleBuildItem;

public final class EventLoopGroupBuildItem extends SimpleBuildItem {

    private final Supplier<EventLoopGroup> bossEventLoopGroup;
    private final Supplier<EventLoopGroup> mainEventLoopGroup;

    public EventLoopGroupBuildItem(Supplier<EventLoopGroup> boss, Supplier<EventLoopGroup> main) {
        this.bossEventLoopGroup = boss;
        this.mainEventLoopGroup = main;
    }

    public Supplier<EventLoopGroup> getBossEventLoopGroup() {
        return bossEventLoopGroup;
    }

    public Supplier<EventLoopGroup> getMainEventLoopGroup() {
        return mainEventLoopGroup;
    }

}
