package org.helioviewer.jhv.display;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Displayer {
    public static Object displaylock = new Object();
    private static Displayer instance = new Displayer();
    private ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();
    //private GL3DComponentFakeInterface gl3dcomponent;
    private final ExecutorService displayPool = Executors.newSingleThreadExecutor();
    private boolean displaying = false;

    public void register(GL3DComponentFakeInterface gl3dcomponent) {
    }

    public static Displayer getSingletonInstance() {
        if (instance == null) {
            throw new NullPointerException("Displayer not initialized");
        }
        return instance;
    }

    public void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DisplayListener renderListener) {
        synchronized (renderListener) {
            listeners.remove(renderListener);
        }
    }

    public void addRenderListener(final RenderListener renderListener) {
        synchronized (renderListener) {
            renderListeners.add(renderListener);
        }
    }

    public void removeRenderListener(final RenderListener listener) {
        synchronized (renderListeners) {
            renderListeners.remove(listener);
        }
    }

    public void render() {
        if (!displaying) {
            displaying = true;
            synchronized (renderListeners) {
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
            displaying = false;
        }

    }

    private void tdisplay() {
        synchronized (displaylock) {
            for (final DisplayListener listener : listeners) {
                listener.display();
            }
        }
    }

    private final class DisplayTask implements Runnable {
        @Override
        public void run() {
            tdisplay();
        }
    }

    public void display() {
        displayPool.submit(new DisplayTask());
    }

    public void removeListeners() {
        this.listeners = new ArrayList<DisplayListener>();
    }

}
