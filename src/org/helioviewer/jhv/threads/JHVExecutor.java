package org.helioviewer.jhv.threads;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class JHVExecutor {

    public static final ExecutorService cachedPool = createCachedPool();
    public static final ScheduledExecutorService reaperPool = createReaperPool();

    private static ExecutorService createCachedPool() {
        ExecutorService service = Executors.newCachedThreadPool();
        shutdownOnDisposal(service);
        return service;
    }

    private static ScheduledExecutorService createReaperPool() {
        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1, new JHVThread.NamedThreadFactory("Reaper"), new ThreadPoolExecutor.DiscardPolicy());
        shutdownOnDisposal(service);
        return service;
    }

    private static void shutdownOnDisposal(ExecutorService es) {
        Runnable shutdownHook =
                new Runnable() {
                    final WeakReference<ExecutorService> executorServiceRef = new WeakReference<>(es);

                    @Override
                    public void run() {
                        ExecutorService executorService = executorServiceRef.get();
                        if (executorService != null) {
                            AccessController.doPrivileged(
                                    (PrivilegedAction<Void>) () -> {
                                        executorService.shutdown();
                                        return null;
                                    });
                        }
                    }
                };

        AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> {
                    Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
                    return null;
                });
    }

}
