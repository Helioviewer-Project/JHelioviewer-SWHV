package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public final class EDTQueue {

    // https://kotek.net/blog/swingutilities.invokeandwait_with_return_value
    public static <E> E invokeAndWait(Callable<E> r) throws InterruptedException, InvocationTargetException {
        if (EventQueue.isDispatchThread()) {
            try {
                return r.call();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }

        AtomicReference<E> ret = new AtomicReference<>();
        AtomicReference<Throwable> thrown = new AtomicReference<>();

        EventQueue.invokeAndWait(() -> {
            try {
                ret.set(r.call());
            } catch (Throwable t) {
                // pass exception to original thread
                thrown.set(t);
            }
        });

        Throwable t = thrown.get();
        if (t == null)
            return ret.get();
        if (t instanceof Error error)
            throw error;
        throw new InvocationTargetException(t);
    }

    public static void invokeAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        invokeAndWait(() -> {
            r.run();
            return null;
        });
    }

    private EDTQueue() {}
}
