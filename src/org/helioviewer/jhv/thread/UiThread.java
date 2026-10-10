package org.helioviewer.jhv.thread;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.annotation.Nonnull;

// The single thread that owns application state. All model mutation happens here.
public final class UiThread {

    public interface Impl {
        // Always enqueues, even when called from the UI thread.
        void post(@Nonnull Runnable task);

        boolean isUiThread();
    }

    private static Impl impl = new AwtUiThread();

    public static void install(@Nonnull Impl _impl) {
        impl = _impl;
    }

    public static void post(@Nonnull Runnable task) {
        impl.post(task);
    }

    public static boolean isUiThread() {
        return impl.isUiThread();
    }

    // Runs inline when already on the UI thread, otherwise posts and blocks until done.
    public static <E> E postAndWait(@Nonnull Callable<E> callable) throws InterruptedException, InvocationTargetException {
        FutureTask<E> task = new FutureTask<>(callable);
        if (impl.isUiThread())
            task.run();
        else
            impl.post(task);

        try {
            return task.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error)
                throw error;
            throw new InvocationTargetException(cause);
        }
    }

    public static void postAndWait(@Nonnull Runnable r) throws InterruptedException, InvocationTargetException {
        postAndWait(() -> {
            r.run();
            return null;
        });
    }

    private UiThread() {}

}
