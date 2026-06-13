package org.helioviewer.jhv.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;

public final class Task {

    public static <T> Future<T> submit(@Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                       @Nonnull Consumer<Throwable> onFailure) {
        return EDTCallbackExecutor.pool.submit(task, onSuccess, onFailure);
    }

    public static <T> Future<T> submit(@Nonnull String logContext, @Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                       @Nonnull FailureHandler onFailure) {
        return EDTCallbackExecutor.pool.submit(task, onSuccess, t -> onFailure.onFailure(logContext, t));
    }

    public static <T> Future<T> submit(@Nonnull String logContext, @Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                       @Nonnull String errorMessage) {
        return submit(logContext, task, onSuccess, (ctx, t) -> defaultOnFailure(ctx, t, errorMessage));
    }

    public static void doNothing(Object ignoredResult) {}

    @FunctionalInterface
    public interface FailureHandler {
        void onFailure(String logContext, Throwable error);
    }

    private static void defaultOnFailure(String logContext, Throwable t, String errorMessage) {
        Log.error(logContext, t);
        Message.err(errorMessage, t.getMessage());
    }

    private Task() {}
}
