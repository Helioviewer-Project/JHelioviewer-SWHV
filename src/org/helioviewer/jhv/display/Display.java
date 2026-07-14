package org.helioviewer.jhv.display;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;

public final class Display {

    public static MapMode mode = MapMode.Orthographic;
    public static boolean multiview = false;
    public static boolean whiteBackground = false;

    public static void setMapMode(MapMode _mode) {
        mode = _mode;
        DisplayController.resetCameras();
    }

    public static GridType gridType = GridType.Viewpoint;

    public static void setGridType(GridType _gridType) {
        gridType = _gridType;
    }

    private static double diskPower = 0.0; // default to logarithmic (mid-scale, most dramatic)

    public static double getDiskPower() {
        return diskPower;
    }

    public static void setDiskPower(double p) {
        diskPower = Math.clamp(p, -1, 1); // -1 = inverse, 0 = logarithmic, 1 = linear
    }

    static int glWidth = 1;
    static int glHeight = 1;
    public static final double[] pixelScale = {1, 1};

    public static void setGLSize(int x, int y, int w, int h) {
        glWidth = w;
        glHeight = h;
        fullViewport = DisplayLayout.fullViewport(x, y, w, h, glHeight);
    }

    private static final Camera camera = new Camera();
    private static final Camera miniCamera = new Camera();

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

    public static void resetViewportZoom() {
        for (Viewport viewport : viewports)
            viewport.zoom = 1;
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
        Viewport[] oldViewports = viewports;
        activeViewport = 0;
        viewports = DisplayLayout.viewports(glWidth, glHeight, countEnabledLayers());
        if (separateViewportZoom) {
            int count = Math.min(oldViewports.length, viewports.length);
            for (int i = 0; i < count; i++)
                viewports[i].zoom = oldViewports[i].zoom;
        } else {
            double zoom = oldViewports[0].zoom;
            for (Viewport viewport : viewports)
                viewport.zoom = zoom;
        }
    }

    public static boolean separateViewportZoom = false;

    public static void setSeparateViewportZoom(boolean separate) {
        separateViewportZoom = separate;
        if (!separateViewportZoom)
            resetViewportZoom();
    }

    private static boolean showCorona = true;

    public static void setShowCorona(boolean _showCorona) {
        showCorona = _showCorona;
    }

    public static boolean getShowCorona() {
        return showCorona;
    }

    private Display() {}
}
