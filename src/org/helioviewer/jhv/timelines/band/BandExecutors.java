package org.helioviewer.jhv.timelines.band;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.thread.AppThread;

final class BandExecutors {

    // Catalog and data requests use separate pools so catalog loading cannot delay data requests in the queue.
    static final int REQUEST_THREADS = 8;

    static ThreadPoolExecutor create(String name, int concurrency) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(concurrency, concurrency, 10000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new AppThread.NamedThreadFactory(name));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private BandExecutors() {}
}
