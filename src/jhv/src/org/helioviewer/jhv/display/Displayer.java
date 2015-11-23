package org.helioviewer.jhv.display;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.layers.Layers;

public class Displayer implements JHVEventHighlightListener {

    public static boolean multiview = false;

    private static Component displayComponent;

    private static int glWidth = 1;
    private static int glHeight = 1;

    public static void setGLSize(int w, int h) {
        glWidth = w;
        glHeight = h;
    }

    public static int getGLWidth() {
        return glWidth;
    }

    public static int getGLHeight() {
        return glHeight;
    }

    private static Camera camera = new Camera();

    public static Camera getCamera() {
        return camera;
    }

    private static Viewport[] viewports = {
        new Viewport(0, 0, 0, 100, 100, true),
        new Viewport(1, 0, 0, 100, 100, false),
        new Viewport(2, 0, 0, 100, 100, false),
        new Viewport(3, 0, 0, 100, 100, false) };
    private static int idxViewport = 0;

    public static void setActiveViewport(int x, int y) {
        for (int i = 0; i < viewports.length; ++i) {
            if (viewports[i].isActive() && viewports[i].contains(x, y)) {
                idxViewport = i;
                break;
            }
        }
    }

    public static Viewport[] getViewports() {
        return viewports;
    }

    public static Viewport getViewport() {
        return viewports[idxViewport];
    }

    private static int countActiveLayers() {
        int ct = 0;
        for (Viewport vp : viewports) {
            if (vp.isActive()) {
                ct++;
            }
        }
        return ct;
    }

    public static void reshapeAll() {
        idxViewport = 0;

        int ct = countActiveLayers();
        switch (ct) {
        case 0:
            reshape();
            break;
        case 1:
            reshape();
            break;
        case 2:
            reshape2();
            break;
        case 3:
            reshape4();
            break;
        case 4:
            reshape4();
            break;
        default:
            reshape();
            break;
        }
    }

    private static void reshape() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(viewports[0], 0, 0, w, h);
        viewports[1] = new Viewport(viewports[1], 0, 0, 0, 0);
        viewports[2] = new Viewport(viewports[2], 0, 0, 0, 0);
        viewports[3] = new Viewport(viewports[3], 0, 0, 0, 0);
    }

    private static void reshape2() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(viewports[0], 0, 0, w / 2, h);
        viewports[1] = new Viewport(viewports[1], w / 2, 0, w / 2, h);
        viewports[2] = new Viewport(viewports[2], 0, 0, 0, 0);
        viewports[3] = new Viewport(viewports[3], 0, 0, 0, 0);
    }

    private static void reshape4() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(viewports[0], 0, 0, w / 2, h / 2);
        viewports[1] = new Viewport(viewports[1], w / 2, 0, w / 2, h / 2);
        viewports[2] = new Viewport(viewports[2], 0, h / 2, w / 2, h / 2);
        viewports[3] = new Viewport(viewports[3], w / 2, h / 2, w / 2, h / 2);
    }

    private static double renderFactor = -1;
    private static boolean toDisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 60, new DisplayTimerListener());

    private Displayer() {
        displayTimer.start();
    }

    public static void render(double f) {
        if (Layers.getActiveView() == null)
            toDisplay = true;
        else
            renderFactor = f;
    }

    public static void render() {
        render(1);
    }

    public static void display() {
        toDisplay = true;
    }

    private static class DisplayTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (toDisplay == true) {
                toDisplay = false;
                displayComponent.repaint();
            }

            if (renderFactor != -1) {
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render(renderFactor);
                }
                renderFactor = -1;
            }
        }
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        display();
    }

    public static void setDisplayComponent(Component component) {
        displayComponent = component;
    }

    private static final HashSet<RenderListener> renderListeners = new HashSet<RenderListener>();

    public static void addRenderListener(final RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public static void removeRenderListener(final RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
