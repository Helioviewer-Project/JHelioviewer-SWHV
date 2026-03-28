package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;

import com.google.common.util.concurrent.FutureCallback;

public final class Tasks {

    @FunctionalInterface
    public interface FailureHandler {
        void onFailure(String logContext, Throwable error);
    }

    private Tasks() {
    }

    public static <T> void submit(@Nonnull String logContext, @Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                  @Nonnull FailureHandler onFailure) {
        EDTCallbackExecutor.pool.submit(task, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                onSuccess.accept(result);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                onFailure.onFailure(logContext, t);
            }
        });
    }

    public static <T> void submit(@Nonnull String logContext, @Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                  @Nonnull String errorMessage) {
        submit(logContext, task, onSuccess, (ctx, t) -> defaultOnFailure(ctx, t, errorMessage));
    }

    private static void defaultOnFailure(String logContext, Throwable t, String errorMessage) {
        Log.error(logContext, t);
        Message.err(errorMessage, t.getMessage());
    }

}
