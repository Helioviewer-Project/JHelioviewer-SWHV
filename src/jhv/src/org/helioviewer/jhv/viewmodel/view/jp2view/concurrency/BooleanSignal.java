package org.helioviewer.jhv.viewmodel.view.jp2view.concurrency;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;

/**
 * Very simple way of signaling between threads. Has no sense of ownership and
 * thus any thread can signal or wait for a signal. In general it is not a
 * problem if many different threads call the signal method, but only one thread
 * should be calling the waitForSignal method, since there is no way to tell
 * which thread will be woken up.
 * 
 * @author caplins
 * 
 */
public class BooleanSignal {

    private final Object lock = new Object();

    /** Signal flag */
    private boolean isSignaled;

    private JP2ImageParameter params;

    /**
     * Default constructor. Assigns the initial value of the isSignaled flag.
     * 
     * @param _intitialVal
     */
    public BooleanSignal(boolean _initialVal) {
        isSignaled = _initialVal;
    }

    /**
     * Used to wait for a signal. Waits until the flag is set, then it resets
     * the flag and returns. The waiting thread can be interrupted and that
     * exception is thrown immediately.
     * 
     * @throws InterruptedException
     */
    public JP2ImageParameter waitForSignal() throws InterruptedException {
        synchronized (lock) {
            while (!isSignaled) {
                lock.wait();
            }
            isSignaled = false;
            return params;
        }
    }

    /**
     * Sets the isSignaled flag and wakes up one waiting thread. Doesn't bother
     * to notifyAll since the first thread woken up resets the flag anyway.
     */
    public void signal(JP2ImageParameter newParams) {
        synchronized (lock) {
            isSignaled = true;
            params = newParams;
            lock.notify();
        }
    }

    /**
     * Returns the signal state.
     * 
     * @return Current signal state
     */
    public boolean isSignaled() {
        return isSignaled;
    }

}
