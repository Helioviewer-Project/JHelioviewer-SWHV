package org.helioviewer.jhv.threads;

import java.awt.EventQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class EventQueueCallbackExecutor extends ForwardingListeningExecutorService {

    private static class EventQueueExecutor implements Executor {

        @Override
        public void execute(@Nonnull Runnable command) {
            EventQueue.invokeLater(command);
        }

    }

    private static final Executor eventQueue = new EventQueueExecutor();

    public static final EventQueueCallbackExecutor pool =
            new EventQueueCallbackExecutor(
                    MoreExecutors.listeningDecorator(
                            MoreExecutors.getExitingExecutorService(
                                    (ThreadPoolExecutor) Executors.newCachedThreadPool())));

    private final ListeningExecutorService delegate;

    public EventQueueCallbackExecutor(ListeningExecutorService _delegate) {
        delegate = _delegate;
    }

    @Override
    public ListeningExecutorService delegate() {
        return delegate;
    }

    public <T> ListenableFuture<T> submit(Callable<T> callable, FutureCallback<T> callback) {
        ListenableFuture<T> futureTask = submit(callable);
        Futures.addCallback(futureTask, callback, eventQueue);
        return futureTask;
    }

}
