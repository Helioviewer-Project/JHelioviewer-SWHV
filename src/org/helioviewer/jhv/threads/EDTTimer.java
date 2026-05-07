package org.helioviewer.jhv.threads;

import javax.swing.Timer;

public final class EDTTimer {

    private Runnable task;
    private final Timer timer;

    public EDTTimer(int delay, Runnable _task) {
        task = _task;
        timer = new Timer(delay, e -> task.run());
    }

    public void setTask(Runnable _task) {
        task = _task;
    }

    public void setDelay(int delay) {
        timer.setDelay(delay);
    }

    public void setInitialDelay(int initialDelay) {
        timer.setInitialDelay(initialDelay);
    }

    public void setRepeats(boolean repeats) {
        timer.setRepeats(repeats);
    }

    public void start() {
        timer.start();
    }

    public void restart() {
        timer.restart();
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

}
