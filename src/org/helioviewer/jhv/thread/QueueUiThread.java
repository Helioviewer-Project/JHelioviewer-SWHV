package org.helioviewer.jhv.thread;

import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

// A UI thread without a toolkit: the thread that calls run() pumps posted tasks forever.
public final class QueueUiThread implements UiThread.Impl {

    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile Thread pump;

    @Override
    public void post(@Nonnull Runnable task) {
        queue.add(task);
    }

    @Override
    public boolean isUiThread() {
        return Thread.currentThread() == pump;
    }

    public void run() {
        pump = Thread.currentThread();
        while (true) {
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("UI thread interrupted", e);
            } catch (Throwable t) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
            }
        }
    }

}
