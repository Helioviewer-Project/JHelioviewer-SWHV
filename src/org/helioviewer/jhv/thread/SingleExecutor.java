package org.helioviewer.jhv.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class SingleExecutor {

    private final ExecutorService executor;

    public SingleExecutor(ThreadFactory factory) {
        executor = Executors.newSingleThreadExecutor(factory);
    }

    public <T> T invokeAndWait(Callable<T> callable) throws InterruptedException, ExecutionException {
        return executor.submit(callable).get();
    }

    public void invokeLater(Runnable runnable) {
        executor.execute(runnable);
    }

}
