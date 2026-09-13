package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;

public final class Task {

    // Work runs on the supplied executor; success and failure callbacks run on the EDT.
    public static <T> Future<T> submit(@Nonnull ExecutorService executor, @Nonnull Callable<T> task,
                                       @Nonnull Consumer<T> onSuccess, @Nonnull Consumer<Throwable> onFailure) {
        FutureTask<T> futureTask = new FutureTask<>(task) {
            @Override
            protected void done() {
                try {
                    T result = get();
                    EventQueue.invokeLater(() -> onSuccess.accept(result));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    EventQueue.invokeLater(() -> onFailure.accept(cause));
                } catch (CancellationException e) {
                    EventQueue.invokeLater(() -> onFailure.accept(e));
                }
            }
        };
        executor.execute(futureTask);
        return futureTask;
    }

    public static <T> Future<T> submitBackground(@Nonnull Callable<T> task, @Nonnull Consumer<T> onSuccess,
                                                 @Nonnull Consumer<Throwable> onFailure) {
        return submit(AppThread.backgroundExecutor, task, onSuccess, onFailure);
    }

    public static <T> Future<T> submitBackground(@Nonnull String logContext, @Nonnull Callable<T> task,
                                                 @Nonnull Consumer<T> onSuccess, @Nonnull FailureHandler onFailure) {
        return submitBackground(task, onSuccess, t -> onFailure.onFailure(logContext, t));
    }

    public static <T> Future<T> submitBackground(@Nonnull String logContext, @Nonnull Callable<T> task,
                                                 @Nonnull Consumer<T> onSuccess, @Nonnull String errorMessage) {
        return submitBackground(logContext, task, onSuccess, (ctx, t) -> defaultOnFailure(ctx, t, errorMessage));
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
