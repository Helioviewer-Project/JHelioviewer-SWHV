package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.concurrent.CountDownLatch;

final class HeadlessEDT {

    private HeadlessEDT() {}

    static void invokeLater(Runnable startup) throws InterruptedException {
        CountDownLatch keepAlive = new CountDownLatch(1);
        EventQueue.invokeLater(() -> {
            startup.run();
            new Pump().run();
        });
        keepAlive.await();
    }

    private static final class Pump extends EventQueue {
        private void run() {
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(this);
            while (true) {
                try {
                    dispatchEvent(getNextEvent());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Headless event loop interrupted", e);
                } catch (Throwable t) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
                }
            }
        }
    }

}
