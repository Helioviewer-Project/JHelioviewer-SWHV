package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.Transform;
import org.helioviewer.jhv.camera.Camera;
//import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.events.JHVEventHighlightListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public class Display implements ActionListener, JHVEventHighlightListener {

    public static final double CAMERA_ZOOM_MULTIPLIER_WHEEL = 2.;
    public static final double CAMERA_ZOOM_MULTIPLIER_BUTTON = 2.;

    public enum DisplayMode {
        Orthographic(GLSLSolarShader.ortho, GridScale.ortho, Transform.transformlatitudinal),
        Latitudinal(GLSLSolarShader.lati, GridScale.lati, Transform.transformlatitudinal),
        LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar, Transform.transformpolar),
        Polar(GLSLSolarShader.polar, GridScale.polar, Transform.transformpolar);

        public final GLSLSolarShader shader;
        public final GridScale scale;
        public final Transform xform;
        public final JRadioButtonMenuItem radio;

        DisplayMode(GLSLSolarShader _shader, GridScale _scale, Transform _xform) {
            shader = _shader;
            scale = _scale;
            xform = _xform;
            radio = new JRadioButtonMenuItem(toString());
            radio.addActionListener(e -> setProjectionMode(this));
        }

    }

    public static DisplayMode mode = DisplayMode.Orthographic;
    public static boolean multiview = false;

    private static void setProjectionMode(DisplayMode newMode) {
        mode = newMode;
        //CameraHelper.zoomToFit(miniCamera);
        miniCamera.reset();
        camera.reset();
    }

    static int glWidth = 1;
    static int glHeight = 1;

    public static void setGLSize(int x, int y, int w, int h) {
        glWidth = w;
        glHeight = h;
        fullViewport = new Viewport(-1, x, y, w, h);
    }

    private static final Camera camera = new Camera();
    private static final Camera miniCamera = new Camera().setViewpointUpdate(UpdateViewpoint.earthFixedDistance);

    public static Camera getCamera() {
        return camera;
    }

    public static Camera getMiniCamera() {
        return miniCamera;
    }

    private static final Viewport[] viewports = {new Viewport(0, 0, 0, 100, 100), null, null, null};
    private static int activeViewport = 0;

    public static Viewport fullViewport = new Viewport(-1, 0, 0, 100, 100);

    public static void setActiveViewport(int x, int y) {
        if (multiview) {
            int len = viewports.length;
            for (int i = 0; i < len; ++i) {
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
            int len = viewports.length;
            for (int i = 0; i < len; ++i) {
                if (ImageLayers.getImageLayerInViewport(i) != null)
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

    private static double decodeFactor = -1;
    private static boolean toDisplay = false;

    public static void start() {
        new Timer(1000 / 60, instance).start();
    }

    public static void render(double f) {
        if (ImageLayers.getNumEnabledImageLayers() == 0)
            toDisplay = true;
        else
            decodeFactor = f;
    }

    public static void display() {
        toDisplay = true;
    }

    public static void handleData(int serial) { // special for ImageLayer.handleData
        if (ImageLayers.getSyncedImageLayers(serial)) {
            ImageViewerGui.getGLWindow().display(); // asap
            toDisplay = false;
        }
    }

    private static int serialNo;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (toDisplay) {
            ImageViewerGui.getGLWindow().display(); // asap
            toDisplay = false;
        }
        if (decodeFactor != -1) {
            ImageLayers.decode(serialNo++, decodeFactor);
            decodeFactor = -1;
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

    private static final Display instance = new Display();

    private Display() {
        JHVRelatedEvents.addHighlightListener(this);
    }

}
