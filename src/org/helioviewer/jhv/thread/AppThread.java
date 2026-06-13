package org.helioviewer.jhv.thread;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

public final class AppThread {

    public static boolean isInterrupted(Throwable t) {
        return t instanceof CancellationException ||
                t instanceof ClosedByInterruptException ||
                t instanceof InterruptedIOException ||
                t instanceof InterruptedException;
    }

    public static Thread create(Runnable task, String name) {
        return new NamedThreadFactory(name).newThread(task);
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
