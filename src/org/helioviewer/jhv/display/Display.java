package org.helioviewer.jhv.display;

import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.GridTransform;
import org.helioviewer.jhv.camera.Camera;
//import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public class Display {

    public enum DisplayMode {
        Orthographic(GLSLSolarShader.ortho, GridScale.ortho, GridTransform.transformlatitudinal),
        Latitudinal(GLSLSolarShader.lati, GridScale.lati, GridTransform.transformlatitudinal),
        LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar, GridTransform.transformpolar),
        Polar(GLSLSolarShader.polar, GridScale.polar, GridTransform.transformpolar);

        public final GLSLSolarShader shader;
        public final GridScale scale;
        public final GridTransform xform;
        public final JRadioButtonMenuItem radio;

        DisplayMode(GLSLSolarShader _shader, GridScale _scale, GridTransform _xform) {
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

    private static final Camera camera = new Camera(UpdateViewpoint.observer);
    private static final Camera miniCamera = new Camera(UpdateViewpoint.earthFixedDistance);

    public static Camera getCamera() {
        return camera;
    }

    public static Camera getMiniCamera() {
        return miniCamera;
    }

    private static Viewport[] viewports = {new Viewport(0, 0, 0, 100, 100)};
    private static int activeViewport = 0;

    public static Viewport fullViewport = new Viewport(-1, 0, 0, 100, 100);

    public static void setActiveViewport(int x, int y) {
        if (multiview) {
            int len = viewports.length;
            for (int i = 0; i < len; ++i) {
                if (viewports[i].contains(x, y)) {
                    activeViewport = i;
                    break;
                }
            }
        }
    }

    public static Viewport getActiveViewport() {
        return viewports[activeViewport];
    }

    public static Viewport getViewport(int idx) {
        return viewports[idx];
    }

    public static Viewport[] getViewports() {
        return viewports;
    }

    private static int countEnabledLayers() {
        int ct = 0;
        if (multiview) {
            for (ImageLayer layer : Layers.getImageLayers()) {
                if (layer.isEnabled()) {
                    ct++;
                    if (ct == 4)
                        break;
                }
            }
        }
        return ct;
    }

    public static void reshapeAll() {
        activeViewport = 0;

        int ct = countEnabledLayers();
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
        viewports = new Viewport[]{new Viewport(0, 0, 0, glWidth, glHeight)};
    }

    private static void reshape2() {
        viewports = new Viewport[]{
                new Viewport(0, 0, 0, glWidth / 2, glHeight),
                new Viewport(1, glWidth / 2, 0, glWidth / 2, glHeight)};
    }

    private static void reshape3() {
        viewports = new Viewport[]{
                new Viewport(0, 0, 0, glWidth / 2, glHeight / 2),
                new Viewport(1, glWidth / 2, 0, glWidth / 2, glHeight / 2),
                new Viewport(2, 0, glHeight / 2, glWidth, glHeight / 2)};
    }

    private static void reshape4() {
        viewports = new Viewport[]{
                new Viewport(0, 0, 0, glWidth / 2, glHeight / 2),
                new Viewport(1, glWidth / 2, 0, glWidth / 2, glHeight / 2),
                new Viewport(2, 0, glHeight / 2, glWidth / 2, glHeight / 2),
                new Viewport(3, glWidth / 2, glHeight / 2, glWidth / 2, glHeight / 2)};
    }

    private static boolean showCorona = true;

    public static void setShowCorona(boolean _showCorona) {
        showCorona = _showCorona;
    }

    public static boolean getShowCorona() {
        return showCorona;
    }

}
