package org.helioviewer.jhv.threads;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;

public class JHVThread {

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
            t.printStackTrace();
        }
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

    public record NamedClassThreadFactory(Class<? extends Thread> clazz, String name) implements ThreadFactory {
        @Nullable
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            try {
                Thread t = clazz.getConstructor(Runnable.class, String.class).newInstance(r, name);
                t.setDaemon(true);
                return t;
            } catch (Exception e) {
                Log.error(e);
            }
            return null;
        }
    }

}
