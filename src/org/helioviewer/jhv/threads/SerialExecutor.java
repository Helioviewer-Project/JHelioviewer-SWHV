package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialExecutor {

    private final ExecutorService executor;

    public SerialExecutor(JHVThread.NamedClassThreadFactory factory) {
        executor = Executors.newSingleThreadExecutor(factory);
    }

    public <T> T invokeAndWait(Callable<T> callable) throws InterruptedException, ExecutionException {
        return executor.submit(callable).get();
    }

}
