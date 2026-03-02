package org.helioviewer.jhv.threads;

import java.awt.EventQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public record EDTCallbackExecutor(ListeningExecutorService delegate) {

    public static final EDTCallbackExecutor pool = new EDTCallbackExecutor(MoreExecutors.listeningDecorator(JHVExecutor.cachedPool));

    private static class EventQueueExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            EventQueue.invokeLater(command);
        }
    }

    private static final Executor eventQueue = new EventQueueExecutor();

    public <T> ListenableFuture<T> submit(Callable<T> callable, FutureCallback<T> callback) {
        ListenableFuture<T> futureTask = delegate.submit(callable);
        Futures.addCallback(futureTask, callback, eventQueue);
        return futureTask;
    }

}
