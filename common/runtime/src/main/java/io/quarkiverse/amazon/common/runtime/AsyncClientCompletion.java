package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.Executor;

/**
 * Completion settings shared by the async clients built by the extensions, including the S3 CRT client.
 * <p>
 * No Vert.x class is reachable from here: the capturing interceptor is only created by recorder methods recorded when the
 * Vert.x extension is present, so that native applications without it do not reference missing classes.
 */
public final class AsyncClientCompletion {

    private AsyncClientCompletion() {
    }

    /**
     * @param pool the Quarkus worker pool, used unless {@code use-future-completion-thread-pool} is disabled
     */
    public static Executor futureCompletionExecutor(AsyncHttpClientConfig config, Executor pool) {
        return new ClassLoaderExecutor(config.advanced().useFutureCompletionThreadPool() ? pool : Runnable::run);
    }

    /**
     * Capture the current ClassLoader and restore it to support dev and test mode, and threads without context ClassLoader
     * such as the AWS CRT event loop threads
     */
    private static final class ClassLoaderExecutor implements Executor {

        private final Executor executor;
        private final ClassLoader contextClassLoader;

        private ClassLoaderExecutor(Executor executor) {
            this.executor = executor;
            this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void execute(Runnable command) {
            final ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                executor.execute(command);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}
