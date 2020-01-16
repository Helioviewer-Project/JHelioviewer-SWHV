package org.helioviewer.jhv.view;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;

public class DecodeExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    private final EventQueueCallbackExecutor executor =
            new EventQueueCallbackExecutor(
                    MoreExecutors.listeningDecorator(
                            new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
                                    new JHVThread.NamedThreadFactory("Decoder"),
                                    new ThreadPoolExecutor.DiscardPolicy())));

    public void decode(Callable<ImageBuffer> callable, FutureCallback<ImageBuffer> callback) {
        blockingQueue.poll();
        executor.submit(callable, callback);
    }

    public void abolish() {
        blockingQueue.poll();
        executor.shutdown();
    }

}
