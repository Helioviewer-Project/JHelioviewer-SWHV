package org.helioviewer.jhv.view;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.threads.JHVThread;

public class DecodeExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("Decoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    public void decode(Runnable r) {
        blockingQueue.poll();
        executor.execute(r);
    }

    public void abolish() {
        try {
            blockingQueue.poll();
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
