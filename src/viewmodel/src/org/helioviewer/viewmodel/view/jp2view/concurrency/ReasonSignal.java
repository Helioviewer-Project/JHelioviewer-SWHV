package org.helioviewer.viewmodel.view.jp2view.concurrency;

/**
 * A class designed to do thread signaling, its like a BooleanSignal except that
 * it gives a reason for the signal.
 * 
 * @author caplins
 * 
 * @param <T>
 */
public class ReasonSignal<T extends Enum<?>> {

    /** Signal flag */
    private volatile boolean isSignaled;

    /** The reason the signal was signaled */
    private volatile T reason = null;

    /**
     * Default constructor. Assigns false to the isSignaled flag.
     */
    public ReasonSignal() {
        isSignaled = false;
    }

    /**
     * Used to wait for a signal. Waits until the flag is set, then it resets
     * the flag and returns. The waiting thread can be interrupted and that
     * exception is thrown immediately.
     * 
     * @throws InterruptedException
     */
    public synchronized T waitForSignal() throws InterruptedException {
        while (!isSignaled)
            this.wait();
        isSignaled = false;
        T ret = reason;
        reason = null;
        return ret;
    }

    /**
     * Sets the isSignaled flag and wakes up one waiting thread. Doesn't bother
     * to notifyAll since the first thread woken up resets the flag anyway.
     */
    public synchronized void signal(T _reason) {
        isSignaled = true;
        reason = _reason;
        this.notify();
    }

    /**
     * Returns the signal state.
     * 
     * @return Current signal state
     */
    public synchronized boolean isSignaled() {
        return isSignaled;
    }

};
