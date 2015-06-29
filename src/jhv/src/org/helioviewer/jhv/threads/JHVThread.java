package org.helioviewer.jhv.threads;

import java.util.concurrent.ThreadFactory;

public class JHVThread {

    public static class NamedThreadFactory implements ThreadFactory {

        private String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, name);
        }
    }

}
