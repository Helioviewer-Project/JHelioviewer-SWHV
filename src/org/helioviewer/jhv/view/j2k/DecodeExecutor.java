package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

class DecodeExecutor {

    private final Cache<DecodeParams, ImageBuffer> decodeCache = CacheBuilder.newBuilder().softValues().build();

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("Decoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    void execute(J2KView view, DecodeParams decodeParams) {
        blockingQueue.poll();

        ImageBuffer imageBuffer = decodeCache.getIfPresent(decodeParams);
        if (imageBuffer == null)
            executor.execute(new J2KDecoder(view, decodeParams, false));
        else
            view.setDataFromDecoder(decodeParams, imageBuffer);
    }

    void addToCache(DecodeParams decodeParams, ImageBuffer imageBuffer) {
        if (decodeParams.complete) {
            try {
                decodeCache.get(decodeParams, () -> imageBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new J2KDecoder(null, null, true));
            executor.shutdown();
            decodeCache.invalidateAll();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
