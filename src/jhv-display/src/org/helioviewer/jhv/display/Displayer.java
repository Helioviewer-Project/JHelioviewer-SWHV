package org.helioviewer.jhv.display;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;

public class Displayer implements JHVEventHighlightListener {
    public static Object displaylock = new Object();
    public static int STATE2D = 1;
    public static int STATE3D = 2;
    private static Displayer instance = new Displayer();
    private ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();
    private final ThreadPoolExecutor displayPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);//Executors.newSingleThreadExecutor();

    private boolean displaying = false;
    private int state = 2;
    public static ArrayList<GL3DVec3d> pointList = new ArrayList<GL3DVec3d>();
    public static int screenScale = 1;

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
        /*synchronized (displaylock)*/ {
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
        if (displayPool.getActiveCount() == 0) {
            displayPool.submit(new DisplayTask());
        }
    }

    public void removeListeners() {
        this.listeners = new ArrayList<DisplayListener>();
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        this.display();
    }
}
