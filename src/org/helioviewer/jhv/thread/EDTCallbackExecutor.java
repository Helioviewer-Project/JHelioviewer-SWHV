package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

record EDTCallbackExecutor(ExecutorService delegate) {

    static final EDTCallbackExecutor pool = new EDTCallbackExecutor(createCachedPool());

    private static ExecutorService createCachedPool() {
        ExecutorService service = Executors.newCachedThreadPool(new AppThread.NamedThreadFactory("Worker"));
        Runnable shutdownHook =
                new Runnable() {
                    private final WeakReference<ExecutorService> executorServiceRef = new WeakReference<>(service);

                    @Override
                    public void run() {
                        ExecutorService executorService = executorServiceRef.get();
                        if (executorService != null)
                            executorService.shutdown();
                    }
                };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "JHV-ShutdownHook"));
        return service;
    }

    <T> Future<T> submit(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        FutureTask<T> futureTask = new FutureTask<>(callable) {
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
        delegate.execute(futureTask);
        return futureTask;
    }
}
