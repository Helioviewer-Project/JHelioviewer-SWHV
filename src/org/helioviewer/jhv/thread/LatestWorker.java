package org.helioviewer.jhv.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;

public final class LatestWorker<T> {

    private record Request<T>(Callable<T> task, Callback<T> callback, int generation) {}

    public interface Callback<T> {
        void onSuccess(T result, boolean fresh);

        default void onFailure(@Nonnull Throwable t, boolean fresh) {
            if (!(t instanceof CancellationException) && !(t instanceof InterruptedException))
                Log.error(t);
        }
    }

    @Nullable
    private final ThreadPoolExecutor worker;
    private final EDTCallbackExecutor executor;

    @Nullable
    private Request<T> pending;
    @Nullable
    private Future<T> running;

    private int generation;
    private boolean abolished;

    public LatestWorker(String name) {
        worker = new ThreadPoolExecutor(
                1, 1, 10000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new AppThread.NamedThreadFactory(name),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        worker.allowCoreThreadTimeOut(true);
        executor = new EDTCallbackExecutor(worker);
    }

    public LatestWorker(ExecutorService sharedWorker) {
        worker = null;
        executor = new EDTCallbackExecutor(sharedWorker);
    }

    public void submit(Callable<T> task, Callback<T> callback) {
        int request = ++generation;
        if (worker != null) {
            executor.submit(task, result -> callback.onSuccess(result, request == generation),
                    t -> callback.onFailure(t, request == generation));
            return;
        }

        pending = new Request<>(task, callback, request);
        if (running == null)
            startPending();
    }

    private void startPending() {
        Request<T> request = pending;
        if (request == null || abolished)
            return;

        pending = null;
        running = executor.submit(request.task(),
                result -> finished(request, result),
                t -> failed(request, t));
    }

    private void finished(Request<T> request, T result) {
        running = null;
        startPending();
        request.callback().onSuccess(result, request.generation() == generation);
    }

    private void failed(Request<T> request, Throwable t) {
        running = null;
        startPending();
        request.callback().onFailure(t, request.generation() == generation);
    }

    public void cancel() {
        generation++;
        if (worker != null)
            worker.getQueue().clear();
        else
            pending = null;
    }

    public void abolish() {
        generation++;
        abolished = true;
        if (worker != null) {
            worker.shutdownNow();
        } else {
            pending = null;
            if (running != null)
                running.cancel(true);
        }
    }

}
