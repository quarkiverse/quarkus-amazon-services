package io.quarkiverse.amazon.common.config;

/** Service adapters own pagination, sessions, retry safety and their client. */
public interface AwsConfigProvider extends AutoCloseable {
    FetchResult fetchAll();

    @Override
    void close();
}
