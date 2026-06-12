package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

record EDTCallbackExecutor(ListeningExecutorService delegate) {

    static final EDTCallbackExecutor pool = new EDTCallbackExecutor(MoreExecutors.listeningDecorator(createCachedPool()));

    private static final Executor eventQueue = EventQueue::invokeLater;

    private static ExecutorService createCachedPool() {
        ExecutorService service = Executors.newCachedThreadPool(new JHVThread.NamedThreadFactory("Worker"));
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

    <T> ListenableFuture<T> submit(Callable<T> callable, FutureCallback<T> callback) {
        ListenableFuture<T> futureTask = delegate.submit(callable);
        Futures.addCallback(futureTask, callback, eventQueue);
        return futureTask;
    }
}
