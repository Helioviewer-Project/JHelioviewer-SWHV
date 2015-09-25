package org.helioviewer.jhv.threads;

import java.util.concurrent.ThreadFactory;

public class JHVThread {

    public static class NamedThreadFactory implements ThreadFactory {

        private final String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new BagThread(r, name);
        }
    }

    public static class BagThread extends Thread {

        private Object var;

        public BagThread(Runnable r, String name) {
            super(r, name);
        }

        public Object getVar() {
            return var;
        }

        public void setVar(Object var) {
            this.var = var;
        }
    }

}
