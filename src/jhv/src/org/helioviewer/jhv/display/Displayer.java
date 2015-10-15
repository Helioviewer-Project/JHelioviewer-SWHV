package org.helioviewer.jhv.display;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewDataHandler;

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

    private static GL3DViewport viewport0 = new GL3DViewport(0, 0, 0, 100, 100, new GL3DObserverCamera(), false, false, true);
    private static GL3DViewport viewport1 = new GL3DViewport(1, 0, 0, 100, 100, viewport0.getCamera(), false, false, false);
    private static GL3DViewport viewport2 = new GL3DViewport(2, 0, 0, 100, 100, viewport0.getCamera(), false, false, false);
    private static GL3DViewport viewport3 = new GL3DViewport(3, 0, 0, 100, 100, viewport0.getCamera(), false, false, false);
    private static GL3DViewport[] viewports = { viewport0, viewport1, viewport2, viewport3 };

    public static GL3DViewport[] getViewports() {
        return viewports;
    }

    public static GL3DViewport getViewport() {
        return viewport0;
    }

    public static void setViewport(GL3DViewport _viewport) {
        viewport0 = _viewport;
    }

    public static void reshapeAll() {

        int ct = 0;
        for (GL3DViewport vp : Displayer.getViewports()) {
            if (vp.isActive()) {
                ct++;
            }
        }
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

        GL3DViewport[] viewports = Displayer.getViewports();
        boolean first = true;
        for (GL3DViewport vp : viewports) {
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
        int h = Displayer.getGLHeight();
        GL3DViewport[] viewports = Displayer.getViewports();
        boolean first = true;
        for (GL3DViewport vp : viewports) {
            if (vp.isActive()) {
                if (first) {
                    vp.getCamera().updateCameraWidthAspect(w / 2 / (double) h);
                    vp.setSize(w / 2, h);
                    vp.setOffset(0, 0);
                    first = false;
                } else {
                    vp.getCamera().updateCameraWidthAspect(w / 2 / (double) h);
                    vp.setSize(w / 2, h);
                    vp.setOffset(w / 2, 0);
                }
            }
        }
    }

    private static void reshape4() {
        int w = Displayer.getGLWidth();
        int h = Displayer.getGLHeight();
        GL3DViewport[] viewports = Displayer.getViewports();
        GL3DViewport vp0 = viewports[0];
        GL3DViewport vp1 = viewports[1];
        GL3DViewport vp2 = viewports[2];
        GL3DViewport vp3 = viewports[3];

        vp0.getCamera().updateCameraWidthAspect(w / (double) h);
        vp0.setSize(w / 2, h / 2);
        vp0.setOffset(0, 0);

        vp1.getCamera().updateCameraWidthAspect(w / (double) h);
        vp1.setSize(w / 2, h / 2);
        vp1.setOffset(w / 2, 0);

        vp2.getCamera().updateCameraWidthAspect(w / (double) h);
        vp2.setSize(w / 2, h / 2);
        vp2.setOffset(0, h / 2);

        vp3.getCamera().updateCameraWidthAspect(w / (double) h);
        vp3.setSize(w / 2, h / 2);
        vp3.setOffset(w / 2, h / 2);
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

    public static void setActiveCamera(GL3DCamera camera) {
        GL3DCamera activeCamera = getViewport().getCamera();
        activeCamera.deactivate();
        camera.activate(activeCamera);
        for (GL3DViewport vp : getViewports()) {
            vp.setCamera(camera);
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
