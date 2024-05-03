package org.helioviewer.jhv.view.j2k;

// Very simple way of signaling between threads. Has no sense of ownership and
// thus any thread can signal or wait for a signal. In general, it is not a
// problem if many threads call the signal method, but only one thread
// should be calling the waitForSignal method, since there is no way to tell
// which thread will be woken up.
class BooleanSignal {

    private final Object lock = new Object();

    private boolean isSignaled;
    private J2KParams.Read params;

    // Used to wait for a signal. Waits until the flag is set, then it resets
    // the flag and returns. The waiting thread can be interrupted and that
    // exception is thrown immediately.
    J2KParams.Read waitForSignal() throws InterruptedException {
        synchronized (lock) {
            while (!isSignaled) {
                lock.wait();
            }
            isSignaled = false;
            // drop reference to params
            J2KParams.Read ret = params;
            params = null;
            return ret;
        }
    }

    // Sets the isSignaled flag and wakes up one waiting thread. Doesn't bother
    // to notifyAll since the first thread woken up resets the flag anyway.
    void signal(J2KParams.Read newParams) {
        synchronized (lock) {
            isSignaled = true;
            params = newParams;
            lock.notify();
        }
    }

    boolean isSignaled() {
        synchronized (lock) {
            return isSignaled;
        }
    }

}
