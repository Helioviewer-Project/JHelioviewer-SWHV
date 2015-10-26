package org.helioviewer.jhv.threads;

import java.util.concurrent.ThreadFactory;

public class JHVThread {

    // this creates daemon threads
    public static class NamedThreadFactory implements ThreadFactory {

        private final String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        }
    }

}
