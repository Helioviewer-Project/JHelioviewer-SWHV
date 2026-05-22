package org.helioviewer.jhv.display;

import java.util.function.Consumer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

public final class DisplayFrame {

    private DisplayFrame() {}

    public enum ViewpointApplyMode {
        RESET,
        KEEP_TRANSFORM
    }

    private static final ViewpointModel viewpointModel = new ViewpointModel(UpdateViewpoint.observer);
    private static final ViewpointModel miniViewpointModel = new ViewpointModel(UpdateViewpoint.earthAt1au);
    private static boolean missingHandlerLogged;
    private static Consumer<Position> renderRequestHandler = _ -> missingRenderRequestHandler();

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

    private static void updateViewpoint(JHVTime time) {
        Display.getCamera().updateViewpoint(viewpointModel.update(time));
    }

    public static void timeChanged(JHVTime time) {
        if (!viewpointModel.getTrackingMode())
            updateViewpoint(time);
    }

    public static void refreshCamera() {
        Display.getCamera().updateViewpoint(viewpointModel.update(Movie.getTime()));
        render(1);
    }

    public static void resetCamera() {
        resetCamera(Display.getCamera(), viewpointModel);
    }

    static void resetCameras() {
        resetCamera(Display.getMiniCamera(), miniViewpointModel);
        resetCamera(Display.getCamera(), viewpointModel);
    }

    public static void zoomMiniToFit() {
        CameraHelper.zoomToFit(Display.getMiniCamera(), miniViewpointModel.getViewpoint());
    }

    private static void resetCamera(Camera camera, ViewpointModel model) {
        Position viewpoint = model.update(Movie.getTime());
        camera.reset(viewpoint);
        CameraHelper.zoomToFit(camera, viewpoint);
        render(1);
    }

    public static RenderView renderView(Position viewpoint) {
        return Display.getCamera().renderView(viewpoint);
    }

    public static MapContext getMapContext(RenderView renderView) {
        return Display.mode.createMapContext(Display.getCamera(), renderView, Display.gridType);
    }

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        renderRequestHandler.accept(getViewpoint());
    }

    public static void setRenderRequestHandler(Consumer<Position> _renderRequestHandler) {
        renderRequestHandler = _renderRequestHandler;
    }

    private static void missingRenderRequestHandler() {
        if (missingHandlerLogged)
            return;
        missingHandlerLogged = true;
        Log.warn("No render request handler installed");
    }
}
