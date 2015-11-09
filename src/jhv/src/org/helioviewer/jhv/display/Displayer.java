package org.helioviewer.jhv.display;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewDataHandler;

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

    public static Dimension getGLSize() {
        return new Dimension(glWidth, glHeight);
    }

    private static Viewport viewport0 = new Viewport(0, 0, 0, 100, 100, new Camera(), true);
    private static Viewport viewport1 = new Viewport(1, 0, 0, 100, 100, viewport0.getCamera(), false);
    private static Viewport viewport2 = new Viewport(2, 0, 0, 100, 100, viewport0.getCamera(), false);
    private static Viewport viewport3 = new Viewport(3, 0, 0, 100, 100, viewport0.getCamera(), false);
    private static Viewport[] viewports = { viewport0, viewport1, viewport2, viewport3 };
    private static Viewport viewport = viewport0;

    public static Viewport[] getViewports() {
        return viewports;
    }

    public static Viewport getViewport() {
        return viewport;
    }

    public static void setViewport(Viewport _viewport) {
        viewport = _viewport;
    }

    public static int countActiveLayers() {
        int ct = 0;
        for (Viewport vp : viewports) {
            if (vp.isActive()) {
                ct++;
            }
        }
        return ct;
    }

    public static void reshapeAll() {
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
        int w = Displayer.getGLWidth();
        int h = Displayer.getGLHeight();

        boolean first = true;
        for (Viewport vp : viewports) {
            if (first && vp.isActive()) {
                vp.getCamera().updateCameraWidthAspect(w / (double) h);
                vp.setSize(w, h);
                vp.setOffset(0, 0);
                first = false;
            } else {
                vp.getCamera().updateCameraWidthAspect(w / (double) h);
                vp.setSize(w, h);
                vp.setOffset(0, 0);
            }
        }
    }

    private static void reshape2() {
        int w = Displayer.getGLWidth();
        int halfw = w / 2;
        int h = Displayer.getGLHeight();
        boolean first = true;

        for (Viewport vp : viewports) {
            if (vp.isActive()) {
                if (first) {
                    vp.getCamera().updateCameraWidthAspect(halfw / (double) h);
                    vp.setSize(halfw, h);
                    vp.setOffset(0, 0);
                    first = false;
                } else {
                    vp.getCamera().updateCameraWidthAspect(halfw / (double) h);
                    vp.setSize(halfw, h);
                    vp.setOffset(halfw, 0);
                }
            }
        }
    }

    private static void reshape4() {
        int w = Displayer.getGLWidth();
        int h = Displayer.getGLHeight();

        viewports[0].getCamera().updateCameraWidthAspect(w / (double) h);
        viewports[0].setSize(w / 2, h / 2);
        viewports[0].setOffset(0, 0);

        viewports[1].getCamera().updateCameraWidthAspect(w / (double) h);
        viewports[1].setSize(w / 2, h / 2);
        viewports[1].setOffset(w / 2, 0);

        viewports[2].getCamera().updateCameraWidthAspect(w / (double) h);
        viewports[2].setSize(w / 2, h / 2);
        viewports[2].setOffset(0, h / 2);

        viewports[3].getCamera().updateCameraWidthAspect(w / (double) h);
        viewports[3].setSize(w / 2, h / 2);
        viewports[3].setOffset(w / 2, h / 2);
    }

    public static void setActiveCamera(Camera camera) {
        for (Viewport vp : viewports) {
            vp.setCamera(camera);
        }
    }

    private static boolean torender = false;
    private static boolean todisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 30, new DisplayTimerListener());

    private Displayer() {
        displayTimer.start();
    }

    public static void render() {
        if (Layers.getActiveView() == null)
            todisplay = true;
        else
            torender = true;
    }

    public static void display() {
        todisplay = true;
    }

    private static class DisplayTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (todisplay == true) {
                todisplay = false;
                displayComponent.repaint();
            }

            if (torender == true) {
                torender = false;
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
        }
    }

    public static final DisplayDataHandler displayDataHandler = new DisplayDataHandler();

    private static class DisplayDataHandler implements ViewDataHandler {
        @Override
        public void handleData(View view, ImageData imageData) {
            view.getImageLayer().setImageData(imageData);
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(view.getImageLayer());
            display();
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
