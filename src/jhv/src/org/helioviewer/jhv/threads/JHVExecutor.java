package org.helioviewer.jhv.threads;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.*;
import java.security.PrivilegedAction;

import javax.swing.SwingWorker;

import sun.awt.AppContext;

public class JHVExecutor {

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

    public static synchronized ExecutorService getJHVWorkersExecutorService(String name, int MAX_WORKER_THREADS) {
        ExecutorService executorService = new ThreadPoolExecutor(MAX_WORKER_THREADS / 2, MAX_WORKER_THREADS,
                                                                 10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                                                                 new JHVThread.NamedThreadFactory("JHVWorker-" + name + "-"));
        shutdownOnDisposal(AppContext.getAppContext(), executorService);
        return executorService;
    }

    private static void shutdownOnDisposal(AppContext appContext, final ExecutorService es) {
        // Don't use ShutdownHook here as it's not enough. We should track
        // AppContext disposal instead of JVM shutdown, see 6799345 for details
        appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent pce) {
                    boolean disposed = (Boolean)pce.getNewValue();
                    if (disposed) {
                        final WeakReference<ExecutorService> executorServiceRef = new WeakReference<ExecutorService>(es);
                        final ExecutorService executorService = executorServiceRef.get();
                        if (executorService != null) {
                            AccessController.doPrivileged(
                                new PrivilegedAction<Void>() {
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

}
