package org.helioviewer.jhv.timelines.band;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.thread.AppThread;

final class HapiRequests {

    // Catalog and data requests use separate pools so catalog loading cannot delay data requests in the queue.
    private static final int CONCURRENCY = 8;

    static ThreadPoolExecutor createExecutor(String name) {
        return new ThreadPoolExecutor(CONCURRENCY, CONCURRENCY, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new AppThread.NamedThreadFactory(name));
    }

    private HapiRequests() {}
}
