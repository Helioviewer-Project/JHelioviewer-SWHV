package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.DisplayView;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

public final class Display {

    private Display() {}

    public enum ViewpointApplyMode {
        RESET,
        KEEP_TRANSFORM
    }

    public static ProjectionMode mode = ProjectionMode.Orthographic;
    public static boolean multiview = false;
    public static boolean whiteBackground = false;

    public static void setProjectionMode(ProjectionMode _mode) {
        mode = _mode;
        //CameraHelper.zoomToFit(miniCamera);
        resetCamera(miniCamera, miniViewpointModel);
        resetCamera(camera, viewpointModel);
    }

    public static GridType gridType = GridType.Viewpoint;

    public static void setGridType(GridType _gridType) {
        gridType = _gridType;
    }

    static int glWidth = 1;
    static int glHeight = 1;
    public static final double[] pixelScale = {1, 1};

    public static void setGLSize(int x, int y, int w, int h) {
        glWidth = w;
        glHeight = h;
        fullViewport = DisplayLayout.fullViewport(x, y, w, h, glHeight);
    }

    private static final ViewpointModel viewpointModel = new ViewpointModel(UpdateViewpoint.observer);
    private static final Camera camera = new Camera(viewpointModel);
    private static final ViewpointModel miniViewpointModel = new ViewpointModel(UpdateViewpoint.earthAt1au);
    private static final Camera miniCamera = new Camera(miniViewpointModel);

    public static Camera getCamera() {
        return camera;
    }

    public static Camera getMiniCamera() {
        return miniCamera;
    }

    public static Position getViewpoint() {
        return viewpointModel.getViewpoint();
    }

    public static UpdateViewpoint getViewpointUpdate() {
        return viewpointModel.getUpdateViewpoint();
    }

    public static boolean getTrackingMode() {
        return viewpointModel.getTrackingMode();
    }

    public static void addViewpointListener(ViewpointModel.Listener listener) {
        viewpointModel.addListener(listener);
    }

    public static void removeViewpointListener(ViewpointModel.Listener listener) {
        viewpointModel.removeListener(listener);
    }

    public static void setViewpointUpdate(UpdateViewpoint updateViewpoint, ViewpointApplyMode mode) {
        viewpointModel.setUpdateViewpoint(updateViewpoint);
        switch (mode) {
            case RESET -> resetCamera();
            case KEEP_TRANSFORM -> updateViewpoint(Movie.getTime());
        }
    }

    public static void setTrackingMode(boolean tracking) {
        if (viewpointModel.setTrackingMode(tracking))
            refreshCamera();
    }

    public static void updateViewpoint(JHVTime time) {
        camera.updateViewpoint(viewpointModel.update(time));
    }

    public static void timeChanged(JHVTime time) {
        if (!viewpointModel.getTrackingMode())
            updateViewpoint(time);
    }

    public static void refreshCamera() {
        camera.refresh(viewpointModel.update(Movie.getTime()));
    }

    public static void resetCamera() {
        resetCamera(camera, viewpointModel);
    }

    private static void resetCamera(Camera camera, ViewpointModel model) {
        camera.reset(model.update(Movie.getTime()));
    }

    public static MapContext getMapContext(Position viewpoint) {
        return getMapContext(camera.displayView(viewpoint));
    }

    public static MapContext getMapContext(DisplayView displayView) {
        return mode.createMapContext(camera, displayView, gridType);
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
        int count = Math.min(oldViewports.length, viewports.length);
        for (int i = 0; i < count; i++)
            viewports[i].zoom = oldViewports[i].zoom;
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
}
