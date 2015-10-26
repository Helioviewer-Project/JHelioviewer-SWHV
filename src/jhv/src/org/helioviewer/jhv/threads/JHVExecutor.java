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

    public static synchronized ExecutorService getWorkersExecutorService(int MAX_WORKER_THREADS) {
       final AppContext appContext = AppContext.getAppContext();
       ExecutorService executorService =
           (ExecutorService) appContext.get(SwingWorker.class);
       if (executorService == null) {
           //this creates daemon threads.
           ThreadFactory threadFactory =
               new ThreadFactory() {
                   final ThreadFactory defaultFactory =
                       Executors.defaultThreadFactory();
                   public Thread newThread(final Runnable r) {
                       Thread thread =
                           defaultFactory.newThread(r);
                       thread.setName("JHVWorker-"
                           + thread.getName());
                       thread.setDaemon(true);
                       return thread;
                   }
               };

           executorService =
               new ThreadPoolExecutor(MAX_WORKER_THREADS/2, MAX_WORKER_THREADS,
                                      10L, TimeUnit.MINUTES,
                                      new LinkedBlockingQueue<Runnable>(),
                                      threadFactory);
           appContext.put(SwingWorker.class, executorService);

           // Don't use ShutdownHook here as it's not enough. We should track
           // AppContext disposal instead of JVM shutdown, see 6799345 for details
           final ExecutorService es = executorService;
           appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
               new PropertyChangeListener() {
                   @Override
                   public void propertyChange(PropertyChangeEvent pce) {
                       boolean disposed = (Boolean)pce.getNewValue();
                       if (disposed) {
                           final WeakReference<ExecutorService> executorServiceRef =
                               new WeakReference<ExecutorService>(es);
                           final ExecutorService executorService =
                               executorServiceRef.get();
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
       return executorService;
   }

}
