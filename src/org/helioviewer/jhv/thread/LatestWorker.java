package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;

// Runs one task at a time, retaining only the latest pending request.
// Callbacks run on the EDT, including stale results with fresh == false.
public final class LatestWorker<T> {

    private static final class Request<T> {
        final Object key;
        final Callable<T> task;
        Callback<T> callback;
        int generation;

        Request(Object _key, Callable<T> _task, Callback<T> _callback, int _generation) {
            key = _key;
            task = _task;
            callback = _callback;
            generation = _generation;
        }
    }

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
    // Keyed tasks remain reusable until their result or failure is delivered on the EDT.
    private final List<Request<T>> outstanding = new ArrayList<>();

    private int generation;
    private boolean disposed;

    // Creates and owns a single-worker executor.
    public LatestWorker(String name) {
        this(AppThread.createIdleExecutor(name, 1, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy()), true);
    }

    // Uses an executor owned by the caller.
    public LatestWorker(ExecutorService _executor) {
        this(_executor, false);
    }

    private LatestWorker(ExecutorService _executor, boolean _ownsExecutor) {
        executor = _executor;
        ownsExecutor = _ownsExecutor;
    }

    public void submit(Callable<T> task, Callback<T> callback) {
        submit(null, task, callback);
    }

    // A matching outstanding key reuses the task with the latest delivery callback.
    public synchronized void submit(@Nullable Object key, Callable<T> task, Callback<T> callback) {
        if (disposed)
            throw new RejectedExecutionException("Worker has been disposed");

        int current = ++generation;
        if (key != null) {
            for (Request<T> request : outstanding) {
                if (key.equals(request.key)) {
                    request.callback = callback;
                    request.generation = current;
                    pending = null;
                    return;
                }
            }
        }
        pending = new Request<>(key, task, callback, current);
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
            if (request.key != null)
                outstanding.add(request);
        }

        try {
            T result = request.task.call();
            EventQueue.invokeLater(() -> complete(request, result, null));
        } catch (Throwable t) {
            EventQueue.invokeLater(() -> complete(request, null, t));
        } finally {
            synchronized (this) {
                scheduled = null;
                if (!disposed)
                    schedule();
            }
        }
    }

    private void complete(Request<T> request, T result, Throwable failure) {
        Callback<T> callback;
        boolean fresh;
        synchronized (this) {
            if (request.key != null)
                outstanding.remove(request);
            callback = request.callback;
            fresh = request.generation == generation;
        }
        if (failure == null)
            callback.onSuccess(result, fresh);
        else
            callback.onFailure(failure, fresh);
    }

    // Drops pending work and marks results stale without interrupting the running task.
    public synchronized void invalidate() {
        generation++;
        pending = null;
        outstanding.clear();
    }

    // Permanently disables this worker, interrupts its task, and shuts down an owned executor.
    public synchronized void dispose() {
        generation++;
        disposed = true;
        pending = null;
        outstanding.clear();
        if (scheduled != null)
            scheduled.cancel(true);
        if (ownsExecutor)
            executor.shutdownNow();
    }

}
