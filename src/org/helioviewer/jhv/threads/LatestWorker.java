package org.helioviewer.jhv.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;

public final class LatestWorker<T> {

    public interface Callback<T> {
        void onSuccess(T result, boolean fresh);

        default void onFailure(@Nonnull Throwable t, boolean fresh) {
            if (!(t instanceof CancellationException) && !(t instanceof InterruptedException))
                Log.error(t);
        }
    }

    private final ThreadPoolExecutor worker;
    private final EDTCallbackExecutor executor;
    private int generation;

    public LatestWorker(String name) {
        worker = new ThreadPoolExecutor(
                1, 1, 10000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new JHVThread.NamedThreadFactory(name),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        worker.allowCoreThreadTimeOut(true);
        executor = new EDTCallbackExecutor(MoreExecutors.listeningDecorator(worker));
    }

    public void submit(Callable<T> task, Callback<T> callback) {
        int request = ++generation;
        executor.submit(task, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                callback.onSuccess(result, request == generation);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                callback.onFailure(t, request == generation);
            }
        });
    }

    public void cancel() {
        generation++;
        worker.getQueue().clear();
    }

    public void abolish() {
        generation++;
        worker.shutdownNow();
    }

}
