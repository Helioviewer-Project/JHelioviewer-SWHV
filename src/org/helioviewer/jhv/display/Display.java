package org.helioviewer.jhv.display;

import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
//import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public class Display {

    public enum ProjectionMode {
        Orthographic(GLSLSolarShader.ortho, GridScale.ortho, null),
        Latitudinal(GLSLSolarShader.lati, GridScale.lati, GridTransform.xformLati),
        LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar, GridTransform.xformPolar),
        Polar(GLSLSolarShader.polar, GridScale.polar, GridTransform.xformPolar);

        public final GLSLSolarShader shader;
        public final GridScale scale;
        private final GridTransform xform;
        public final JRadioButtonMenuItem radio;

        ProjectionMode(GLSLSolarShader _shader, GridScale _scale, GridTransform _xform) {
            shader = _shader;
            scale = _scale;
            xform = _xform;
            radio = new JRadioButtonMenuItem(toString());
            radio.addActionListener(e -> setProjectionMode(this));
        }

        public Vec2 transform(Quat q, Vec3 v) {
            return xform.transform(q.rotateVector(v), scale);
        }

        public Vec3 transformInverse(Quat q, Vec2 pt) {
            return q.rotateInverseVector(xform.transformInverse(pt));
        }

    }

    public static ProjectionMode mode = ProjectionMode.Orthographic;
    public static boolean multiview = false;

    private static void setProjectionMode(ProjectionMode _mode) {
        mode = _mode;
        //CameraHelper.zoomToFit(miniCamera);
        miniCamera.reset();
        camera.reset();
    }

    public static GridType gridType = GridType.Viewpoint;

    public static void setGridType(GridType _gridType) {
        gridType = _gridType;
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
                    if (ct == 6)
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
            case 2 -> reshape2();
            case 3 -> reshape3();
            case 4 -> reshape4();
            case 5 -> reshape5();
            case 6 -> reshape6();
            default -> reshape();
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

    private static void reshape5() {
        viewports = new Viewport[]{
                new Viewport(0, 0, 0, glWidth / 3, glHeight / 2),
                new Viewport(1, glWidth / 3, 0, glWidth / 3, glHeight / 2),
                new Viewport(2, 2 * glWidth / 3, 0, glWidth / 3, glHeight / 2),
                new Viewport(3, 0, glHeight / 2, glWidth / 2, glHeight / 2),
                new Viewport(4, glWidth / 2, glHeight / 2, glWidth / 2, glHeight / 2)};
    }

    private static void reshape6() {
        viewports = new Viewport[]{
                new Viewport(0, 0, 0, glWidth / 3, glHeight / 2),
                new Viewport(1, glWidth / 3, 0, glWidth / 3, glHeight / 2),
                new Viewport(2, 2 * glWidth / 3, 0, glWidth / 3, glHeight / 2),
                new Viewport(3, 0, glHeight / 2, glWidth / 3, glHeight / 2),
                new Viewport(4, glWidth / 3, glHeight / 2, glWidth / 3, glHeight / 2),
                new Viewport(5, 2 * glWidth / 3, glHeight / 2, glWidth / 3, glHeight / 2)};
    }

    private static boolean showCorona = true;

    public static void setShowCorona(boolean _showCorona) {
        showCorona = _showCorona;
    }

    public static boolean getShowCorona() {
        return showCorona;
    }

}
