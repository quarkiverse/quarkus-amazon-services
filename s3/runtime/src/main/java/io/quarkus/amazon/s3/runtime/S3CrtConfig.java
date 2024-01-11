package io.quarkus.amazon.s3.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface S3CrtConfig {

    /**
     * Configure the starting buffer size the client will use to buffer the parts downloaded from S3.
     */
    @ConfigDocDefault("Equal to the resolved part size * 10")
    Optional<Long> initialReadBufferSizeInBytes();

    /**
     * Specifies the maximum number of S3 connections that should be established during a transfer.
     */
    @ConfigDocDefault("")
    Optional<Integer> maxConcurrency();

    /**
     * Sets the minimum part size for transfer parts.
     */
    @ConfigDocDefault("8MB")
    Optional<Long> minimumPartSizeInBytes();

    /**
     * The target throughput for transfer requests.
     */
    @ConfigDocDefault("10")
    Optional<Double> targetThroughputInGbps();
}
