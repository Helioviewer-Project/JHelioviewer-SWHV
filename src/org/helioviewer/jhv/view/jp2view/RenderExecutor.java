package org.helioviewer.jhv.view.jp2view;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;

class RenderExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
                                                                    new JHVThread.NamedThreadFactory("Render"),
                                                                    new ThreadPoolExecutor.DiscardPolicy());

    void execute(JP2View view, Camera camera, Viewport vp, int frame, double factor) {
        // order is important, this will signal reader
        ImageParams params = view.calculateParams(camera, vp, frame, factor);
        AtomicBoolean status = view.getCacheStatus().getFrameStatus(frame, params.resolution.level);
        if (status == null)
            return;

        execute(view, params, !status.get());
    }

    void execute(JP2View view, ImageParams params, boolean discard) {
        blockingQueue.poll();
        executor.execute(new J2KRender(view, params, discard, false));
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new J2KRender(null, null, false, true));
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
