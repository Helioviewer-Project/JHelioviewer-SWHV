package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nonnull;

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

    private final ExecutorService executor;
    private final boolean ownsExecutor;

    private Request<T> pending;
    private Future<?> scheduled;

    private int generation;
    private boolean abolished;

    public LatestWorker(String name) {
        this(AppThread.createIdleExecutor(name, 1, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy()), true);
    }

    public LatestWorker(ExecutorService _executor) {
        this(_executor, false);
    }

    private LatestWorker(ExecutorService _executor, boolean _ownsExecutor) {
        executor = _executor;
        ownsExecutor = _ownsExecutor;
    }

    public synchronized void submit(Callable<T> task, Callback<T> callback) {
        if (abolished)
            throw new RejectedExecutionException("Worker has been abolished");

        pending = new Request<>(task, callback, ++generation);
        schedule();
    }

    private void schedule() {
        if (scheduled == null && pending != null)
            scheduled = executor.submit(this::runPending);
    }

    private void runPending() {
        Request<T> request;
        synchronized (this) {
            request = pending;
            pending = null;
            if (request == null) {
                scheduled = null;
                return;
            }
        }

        try {
            T result = request.task().call();
            EventQueue.invokeLater(() ->
                    request.callback().onSuccess(result, isFresh(request.generation())));
        } catch (Throwable t) {
            EventQueue.invokeLater(() ->
                    request.callback().onFailure(t, isFresh(request.generation())));
        } finally {
            synchronized (this) {
                scheduled = null;
                if (!abolished)
                    schedule();
            }
        }
    }

    private synchronized boolean isFresh(int request) {
        return request == generation;
    }

    public synchronized void cancel() {
        generation++;
        pending = null;
    }

    public synchronized void abolish() {
        generation++;
        abolished = true;
        pending = null;
        if (scheduled != null)
            scheduled.cancel(true);
        if (ownsExecutor)
            executor.shutdownNow();
    }

}
