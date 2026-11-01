package org.helioviewer.jhv.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class UiTimerTest {

    public static void main(String[] args) throws Exception {
        Queue queue = new Queue();
        UiThread.install(queue);
        AtomicInteger calls = new AtomicInteger();
        UiTimer timer = new UiTimer(10, calls::incrementAndGet);
        try {
            timer.start();
            Runnable stopped = queue.next("Timer did not post");
            check(queue.tasks.poll(100, TimeUnit.MILLISECONDS) == null, "Timer did not coalesce queued ticks");
            timer.stop();
            stopped.run();
            check(calls.get() == 0, "Stopped timer fired a queued tick");

            timer.start();
            Runnable restarted = queue.next("Timer did not restart");
            timer.restart();
            timer.setRepeats(false);
            restarted.run();
            check(calls.get() == 0, "Restarted timer fired the old generation");
            queue.next("Restart lost next tick").run();
            check(calls.get() == 1 && !timer.isRunning(), "One-shot timer did not stop");

            boolean[] queuedDuringCallback = {false};
            timer.setRepeats(true);
            timer.setTask(() -> {
                calls.incrementAndGet();
                try {
                    queuedDuringCallback[0] = queue.tasks.poll(100, TimeUnit.MILLISECONDS) != null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } finally {
                    timer.stop();
                }
            });
            timer.start();
            queue.next("Slow callback was not posted").run();
            check(!queuedDuringCallback[0], "Timer queued a tick while its callback was running");
            check(calls.get() == 2 && !timer.isRunning(), "Slow callback did not stop");
        } finally {
            timer.stop();
        }
        System.out.println("UiTimerTest passed");
    }

    private static final class Queue implements UiThread.Impl {
        private final Thread owner = Thread.currentThread();
        private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

        @Override
        public void post(Runnable task) {
            tasks.add(task);
        }

        @Override
        public boolean isUiThread() {
            return Thread.currentThread() == owner;
        }

        Runnable next(String failure) throws InterruptedException {
            Runnable task = tasks.poll(5, TimeUnit.SECONDS);
            check(task != null, failure);
            return task;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
