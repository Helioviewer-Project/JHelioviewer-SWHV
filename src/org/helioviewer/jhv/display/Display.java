package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;

public class Display {

    public static ProjectionMode mode = ProjectionMode.Orthographic;
    public static boolean multiview = false;

    public static void setProjectionMode(ProjectionMode _mode) {
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
        fullViewport = DisplayLayout.fullViewport(x, y, w, h, glHeight);
    }

    private static final Camera camera = new Camera(UpdateViewpoint.observer);
    private static final Camera miniCamera = new Camera(UpdateViewpoint.earthFixedDistance);

    public static Camera getCamera() {
        return camera;
    }

    public static Camera getMiniCamera() {
        return miniCamera;
    }

    private static Viewport[] viewports = {DisplayLayout.viewport(0, 0, 0, 100, 100, glHeight)};
    private static int activeViewport = 0;

    public static Viewport fullViewport = DisplayLayout.fullViewport(0, 0, 100, 100, glHeight);

    private static Viewport findViewport(int x, int y) {
        if (!multiview)
            return viewports[0];

        int len = viewports.length;
        for (Viewport viewport : viewports) {
            if (viewport.contains(x, y)) {
                return viewport;
            }
        }
        return viewports[activeViewport];
    }

    public static Viewport setActiveViewport(int x, int y) {
        Viewport vp = findViewport(x, y);
        activeViewport = vp.idx;
        return vp;
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
        viewports = DisplayLayout.viewports(glWidth, glHeight, countEnabledLayers());
    }

    private static boolean showCorona = true;

    public static void setShowCorona(boolean _showCorona) {
        showCorona = _showCorona;
    }

    public static boolean getShowCorona() {
        return showCorona;
    }
}
