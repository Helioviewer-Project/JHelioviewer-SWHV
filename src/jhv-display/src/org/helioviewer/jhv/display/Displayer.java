package org.helioviewer.jhv.display;

// import java.awt.EventQueue;
import java.util.ArrayList;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;

public class Displayer implements JHVEventHighlightListener {

    private final static Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

    public static Object displaylock = new Object();
    private ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();

    private boolean displaying = false;

    public static int STATE2D = 1;
    public static int STATE3D = 2;
    private int state = STATE3D;

    public static ArrayList<GL3DVec3d> pointList = new ArrayList<GL3DVec3d>();
    public static int screenScale = 1;

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

    public void display() {
/*        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() { */
                for (final DisplayListener listener : listeners) {
                    listener.display();
                }
/*            }
        });
*/    }

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
