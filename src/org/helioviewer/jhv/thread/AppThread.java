package org.helioviewer.jhv.thread;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;

public final class AppThread {

    public static boolean isInterrupted(Throwable t) {
        return t instanceof CancellationException ||
                t instanceof ClosedByInterruptException ||
                t instanceof InterruptedIOException ||
                t instanceof InterruptedException;
    }

    public static void afterExecute(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?> future) {
            try {
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException e) {
                t = e;
            } catch (ExecutionException e) {
                t = e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ??? ignore/reset
            }
        }
        if (t != null) {
            if (isInterrupted(t))
                return;
            Log.error(t);
        }
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
