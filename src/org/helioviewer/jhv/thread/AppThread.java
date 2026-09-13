package org.helioviewer.jhv.thread;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public final class AppThread {

    // Shared, unbounded daemon pool. Workers are created on demand and expire after 60 idle seconds.
    static final ExecutorService backgroundExecutor = createBackgroundExecutor();

    private static ExecutorService createBackgroundExecutor() {
        ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("Worker"));
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown, "JHV-ShutdownHook"));
        return executor;
    }

    public static boolean isInterrupted(Throwable t) {
        return t instanceof CancellationException ||
                t instanceof ClosedByInterruptException ||
                t instanceof InterruptedIOException ||
                t instanceof InterruptedException;
    }

    public static Thread create(Runnable task, String name) {
        return new NamedThreadFactory(name).newThread(task);
    }

    // Separate daemon pool with bounded concurrency and a 10-second idle timeout.
    public static ThreadPoolExecutor createIdleExecutor(String name, int concurrency) {
        return createIdleExecutor(name, concurrency, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.AbortPolicy());
    }

    public static ThreadPoolExecutor createIdleExecutor(String name, int concurrency,
                                                    BlockingQueue<Runnable> queue, RejectedExecutionHandler rejectionHandler) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(concurrency, concurrency, 10L, TimeUnit.SECONDS,
                queue, new NamedThreadFactory(name), rejectionHandler);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    // this creates daemon threads
    public record NamedThreadFactory(String name) implements ThreadFactory {
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        }
    }

    private AppThread() {}
}
