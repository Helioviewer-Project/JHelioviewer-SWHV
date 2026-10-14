package org.helioviewer.jhv.thread;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

// Coalesces ticks until the UI-thread callback finishes; stop() cancels queued ticks.
// Driven from the UI thread; only tick() runs on the scheduler thread.
public final class UiTimer {

    private static final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> AppThread.create(r, "JHV-UiTimer"));

    static {
        scheduler.setRemoveOnCancelPolicy(true);
    }

    private Runnable task;
    private int delay;
    private boolean repeats = true;
    private ScheduledFuture<?> scheduled;
    private volatile boolean running;
    private volatile int generation;
    private final AtomicBoolean pending = new AtomicBoolean();

    public UiTimer(int _delay, @Nonnull Runnable _task) {
        delay = _delay;
        task = _task;
    }

    public void setTask(@Nonnull Runnable _task) {
        task = _task;
    }

    // Takes effect at once when running.
    public void setDelay(int _delay) {
        delay = _delay;
        if (running)
            schedule();
    }

    public void setRepeats(boolean _repeats) {
        repeats = _repeats;
    }

    public void start() {
        if (!running) {
            running = true;
            schedule();
        }
    }

    public void restart() {
        stop();
        start();
    }

    public void stop() {
        running = false;
        generation++;
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void schedule() {
        if (scheduled != null)
            scheduled.cancel(false);
        scheduled = scheduler.scheduleWithFixedDelay(this::tick, delay, delay, TimeUnit.MILLISECONDS);
    }

    // Scheduler thread: coalesce into one queued tick.
    private void tick() {
        if (!running || !pending.compareAndSet(false, true))
            return;
        int gen = generation;
        UiThread.post(() -> fire(gen));
    }

    // UI thread: run unless stopped or restarted after the tick was queued.
    private void fire(int gen) {
        try {
            if (gen != generation || !running)
                return;
            if (!repeats)
                stop();
            task.run();
        } finally {
            pending.set(false);
        }
    }

}
