package org.helioviewer.jhv.threads;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class EDTQueue {

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
        AtomicReference<Exception> except = new AtomicReference<>();

        EventQueue.invokeAndWait(() -> {
            try {
                ret.set(r.call());
            } catch (Exception e) {
                // pass exception to original thread
                except.set(e);
            }
        });

        Exception e = except.get();
        if (e != null) // there was an exception on EDT thread, rethrow it
            throw new InvocationTargetException(e);
        else
            return ret.get();
    }

}
