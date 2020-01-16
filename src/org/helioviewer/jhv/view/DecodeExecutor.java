package org.helioviewer.jhv.view;

import java.awt.EventQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DecodeExecutor {

    private static class EventQueueExecutor implements Executor {

        @Override
        public void execute(@Nonnull Runnable command) {
            EventQueue.invokeLater(command);
        }

    }

    private static final Executor eqExecutor = new EventQueueExecutor();

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
                    new JHVThread.NamedThreadFactory("Decoder"),
                    new ThreadPoolExecutor.DiscardPolicy()));

    public void decode(Callable<ImageBuffer> callable, FutureCallback<ImageBuffer> callback) {
        blockingQueue.poll();
        ListenableFuture<ImageBuffer> futureTask = executor.submit(callable);
        Futures.addCallback(futureTask, callback, eqExecutor);
    }

    public void decode(Runnable r) {
        blockingQueue.poll();
        executor.execute(r);
    }

    public void abolish() {
        blockingQueue.poll();
        executor.shutdown();
    }

}
