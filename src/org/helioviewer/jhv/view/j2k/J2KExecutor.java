package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;

public class J2KExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("J2KDecoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    void decode(DecodeParams decodeParams) {
        blockingQueue.poll();
        executor.execute(new J2KDecoder(decodeParams));
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new J2KDecoder(null));
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
