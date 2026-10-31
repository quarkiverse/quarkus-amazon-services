package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.Executor;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

/** Dispatches final results to the caller's event-loop context without changing HTTP attempt processing. */
public final class VertxContextAwareExecutor implements Executor {
    private final Context context;

    private VertxContextAwareExecutor(Context context) {
        this.context = context;
    }

    /**
     * @return the executor completing on the caller's context, or {@code null} if the caller is not on an event loop
     */
    static Executor capture(boolean enabled) {
        if (!enabled || !Context.isOnEventLoopThread()) {
            // Worker contexts serialize their callbacks: dispatching a result there can deadlock a blocking caller.
            return null;
        }
        Context context = Vertx.currentContext();
        if (context == null || !context.isEventLoopContext()) {
            return null;
        }
        return new VertxContextAwareExecutor(context);
    }

    @Override
    public void execute(Runnable command) {
        context.runOnContext(ignored -> command.run());
    }
}
