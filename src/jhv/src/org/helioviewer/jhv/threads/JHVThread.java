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
            return new Thread(r, name);
        }
    }

    public static class J2KRenderThread extends Thread {
        private Object var;

        public J2KRenderThread(Runnable r, String name) {
            super(r, name);
        }

        public Object getVar() {
            return var;
        }

        public void setVar(Object var) {
            this.var = var;
        }
    }

    public static class J2KRenderThreadFactory implements ThreadFactory {

        private final String name;

        public J2KRenderThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new J2KRenderThread(r, name);
        }
    }
}
