package org.helioviewer.jhv.threads;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JHVThread {

    public static boolean isInterrupted(Throwable t) {
        return t instanceof CancellationException ||
                t instanceof ClosedByInterruptException ||
                t instanceof InterruptedIOException ||
                t instanceof InterruptedException;
    }

    public static void afterExecute(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
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
    public static class NamedThreadFactory implements ThreadFactory {

        private final String name;

        public NamedThreadFactory(String _name) {
            name = _name;
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        }
    }

    public static class NamedClassThreadFactory implements ThreadFactory {

        private final Class<? extends Thread> clazz;
        private final String name;

        public NamedClassThreadFactory(Class<? extends Thread> _clazz, String _name) {
            clazz = _clazz;
            name = _name;
        }

        @Nullable
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            try {
                Thread t = clazz.getConstructor(Runnable.class, String.class).newInstance(r, name);
                t.setDaemon(true);
                return t;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
