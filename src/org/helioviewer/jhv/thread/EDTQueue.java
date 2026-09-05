package org.helioviewer.jhv.thread;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public final class EDTQueue {

    public static <E> E invokeAndWait(Callable<E> callable) throws InterruptedException, InvocationTargetException {
        FutureTask<E> task = new FutureTask<>(callable);
        if (EventQueue.isDispatchThread())
            task.run();
        else
            EventQueue.invokeAndWait(task);

        try {
            return task.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error)
                throw error;
            throw new InvocationTargetException(cause);
        }
    }

    public static void invokeAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        invokeAndWait(() -> {
            r.run();
            return null;
        });
    }

    private EDTQueue() {}
}
