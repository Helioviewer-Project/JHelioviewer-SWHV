package org.helioviewer.jhv.base;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class EventDispatchQueue {

   // http://kotek.net/blog/swingutilities.invokeandwait_with_return_value
    public static <E> E invokeAndWait(final Callable<E> r) throws InterruptedException, InvocationTargetException {
        final AtomicReference<E> ret = new AtomicReference<E>();
        final AtomicReference<Exception> except = new AtomicReference<Exception>();

        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    ret.set(r.call());
                } catch (Exception e) {
                    //pass exception to original thread
                    except.set(e);
                }
            }});

        if (except.get() != null)
            //there was an exception in EDT thread, rethrow it
            throw new RuntimeException(except.get());
        else
            return ret.get();
    }

}
