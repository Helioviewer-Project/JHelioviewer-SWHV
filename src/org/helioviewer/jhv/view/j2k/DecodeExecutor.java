package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.view.j2k.image.ImageParams;

class DecodeExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("Decoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    void execute(J2KView view, int serialNo, int frame, double pixFactor, double factor) {
        // order is important, this will signal reader
        ImageParams params = view.calculateParams(serialNo, frame, pixFactor, factor);
        AtomicBoolean status = view.getCacheStatus().getFrameStatus(frame, params.decodeParams.resolution.level);
        if (status == null)
            return;

        execute(view, params, !status.get());
    }

    void execute(J2KView view, ImageParams params, boolean discard) {
        blockingQueue.poll();
        executor.execute(new J2KDecoder(view, params, discard, false));
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new J2KDecoder(null, null, false, true));
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
