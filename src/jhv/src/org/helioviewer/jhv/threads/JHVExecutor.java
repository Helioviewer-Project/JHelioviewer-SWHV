package org.helioviewer.jhv.threads;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JHVExecutor {

/*
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import sun.awt.AppContext;

    public static synchronized void setSwingWorkersExecutorService(int MAX_WORKER_THREADS) {

                final AppContext appContext = AppContext.getAppContext();
                ExecutorService executorService = (ExecutorService) appContext.get(SwingWorker.class);
                if (executorService == null) {
                    executorService = new ThreadPoolExecutor(MAX_WORKER_THREADS / 2, MAX_WORKER_THREADS,
                                                             10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                                                             new JHVThread.NamedThreadFactory("JHVWorker-Swing-"));
                    shutdownOnDisposal(appContext, executorService);

                    appContext.put(SwingWorker.class, executorService);
                }
    }

    private static void shutdownOnDisposal(AppContext appContext, final ExecutorService es) {
        // Don't use ShutdownHook here as it's not enough. We should track
        // AppContext disposal instead of JVM shutdown, see 6799345 for details
        appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        boolean disposed = (Boolean) pce.getNewValue();
                        if (disposed) {
                            final WeakReference<ExecutorService> executorServiceRef = new WeakReference<ExecutorService>(es);
                            final ExecutorService executorService = executorServiceRef.get();
                            if (executorService != null) {
                                AccessController.doPrivileged(
                                        new PrivilegedAction<Void>() {
                                            @Override
                                            public Void run() {
                                                executorService.shutdown();
                                                return null;
                                            }
                                        }
                                        );
                            }
                        }
                    }
                }
                );
    }
*/

    public static ExecutorService getJHVWorkersExecutorService(String name, int MAX_WORKER_THREADS) {
        ExecutorService executorService = new ThreadPoolExecutor(MAX_WORKER_THREADS / 2, MAX_WORKER_THREADS,
                10L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new JHVThread.NamedThreadFactory("JHVWorker-" + name)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                JHVThread.afterExecute(r, t);
            }
        };
        shutdownOnDisposal(executorService);
        return executorService;
    }

    private static void shutdownOnDisposal(ExecutorService es) {
        Runnable shutdownHook =
            new Runnable() {
                final WeakReference<ExecutorService> executorServiceRef = new WeakReference<>(es);
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
