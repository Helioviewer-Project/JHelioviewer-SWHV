package org.helioviewer.jhv.view.fits;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.threads.JHVThread;

public class FITSDecodeExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("FITSDecoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    void decode(FITSView view, Position viewpoint) {
        blockingQueue.poll();
        executor.execute(new FITSDecoder(view, viewpoint));
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new FITSDecoder(null, null));
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
