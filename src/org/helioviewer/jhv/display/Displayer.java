package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;

import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
// import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.UpdateViewpoint;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.data.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public class Displayer implements JHVEventHighlightListener {

    public static final double CAMERA_ZOOM_MULTIPLIER_WHEEL = 2.;
    public static final double CAMERA_ZOOM_MULTIPLIER_BUTTON = 2.;

    public enum DisplayMode {
        Orthographic(GLSLSolarShader.ortho, GridScale.ortho), Latitudinal(GLSLSolarShader.lati, GridScale.lati),
        LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar), Polar(GLSLSolarShader.polar, GridScale.polar);

        public final GLSLSolarShader shader;
        public final GridScale scale;
        public final JRadioButtonMenuItem radio;

        DisplayMode(GLSLSolarShader _shader, GridScale _scale) {
            shader = _shader;
            scale = _scale;
            radio = new JRadioButtonMenuItem(toString());
            radio.addActionListener(e -> setProjectionMode(this));
        }

    }

    public static DisplayMode mode = DisplayMode.Orthographic;
    public static boolean multiview = false;

    private static void setProjectionMode(DisplayMode newMode) {
        mode = newMode;
        // CameraHelper.zoomToFit(miniCamera);
        // ImageViewerGui.getRenderableMiniview().getCamera().reset();
        camera.reset();
    }

    private static UpdateViewpoint updateViewpoint = UpdateViewpoint.observer;

    public static void setViewpointUpdate(UpdateViewpoint update) {
        updateViewpoint = update;
    }

    public static UpdateViewpoint getUpdateViewpoint() {
        if (mode == DisplayMode.Orthographic)
            return updateViewpoint;
        return UpdateViewpoint.earthFixedDistance;
    }

    static int glWidth = 1;
    static int glHeight = 1;

    public static void setGLSize(int x, int y, int w, int h) {
        glWidth = w;
        glHeight = h;
        fullViewport = new Viewport(-1, x, y, w, h);
    }

    private static final Camera camera = new Camera();
    private static final Camera miniCamera = new Camera();

    public static Camera getCamera() {
        return camera;
    }

    public static Camera getMiniCamera() {
        return miniCamera;
    }

    private static final Viewport[] viewports = { new Viewport(0, 0, 0, 100, 100), null, null, null };
    private static int activeViewport = 0;

    public static Viewport fullViewport = new Viewport(-1, 0, 0, 100, 100);

    public static void setActiveViewport(int x, int y) {
        if (multiview) {
            for (int i = 0; i < viewports.length; ++i) {
                Viewport vp = viewports[i];
                if (vp != null && vp.contains(x, y)) {
                    activeViewport = i;
                    break;
                }
            }
        }
    }

    public static Viewport getActiveViewport() {
        return viewports[activeViewport];
    }

    public static Viewport[] getViewports() {
        return viewports;
    }

    private static int countActiveLayers() {
        int ct = 0;
        if (multiview) {
            for (int i = 0; i < viewports.length; ++i) {
                if (ImageViewerGui.getRenderableContainer().getImageLayerInViewport(i) != null)
                    ct++;
            }
        }
        return ct;
    }

    public static void reshapeAll() {
        activeViewport = 0;

        int ct = countActiveLayers();
        switch (ct) {
        case 2:
            reshape2();
            break;
        case 3:
            reshape3();
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

        viewports[0] = new Viewport(0, 0, 0, w, h);
        viewports[1] = null;
        viewports[2] = null;
        viewports[3] = null;
    }

    private static void reshape2() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h);
        viewports[2] = null;
        viewports[3] = null;
    }

    private static void reshape3() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h / 2);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h / 2);
        viewports[2] = new Viewport(2, 0, h / 2, w, h / 2);
        viewports[3] = null;
    }

    private static void reshape4() {
        int w = glWidth;
        int h = glHeight;

        viewports[0] = new Viewport(0, 0, 0, w / 2, h / 2);
        viewports[1] = new Viewport(1, w / 2, 0, w / 2, h / 2);
        viewports[2] = new Viewport(2, 0, h / 2, w / 2, h / 2);
        viewports[3] = new Viewport(3, w / 2, h / 2, w / 2, h / 2);
    }

    private static double renderFactor = -1;
    private static boolean toDisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 60, new DisplayTimerListener());

    private Displayer() {
        JHVRelatedEvents.addHighlightListener(this);
        displayTimer.setCoalesce(true);
        displayTimer.start();
    }

    public static void render(double f) {
        if (Layers.getActiveView() == null || ImageViewerGui.getRenderableContainer().getNumEnabledImageLayers() == 0)
            toDisplay = true;
        else
            renderFactor = f;
    }

    public static void display() {
        toDisplay = true;
    }

    private static class DisplayTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (toDisplay) {
                // ImageViewerGui.getGLWindow().display();
                ImageViewerGui.getGLComponent().repaint();
                toDisplay = false;
            }
            if (renderFactor != -1) {
                Layers.setRender(camera, renderFactor);
                renderFactor = -1;
            }
        }
    }

    @Override
    public void eventHightChanged() {
        display();
    }

    private static boolean showCorona = true;

    public static void setShowCorona(boolean _showCorona) {
        showCorona = _showCorona;
    }

    public static boolean getShowCorona() {
        return showCorona;
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
