package org.helioviewer.jhv.display;

import java.util.function.Consumer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

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
        updateViewpoint(Movie.getTime());
        render(1);
    }

    public static void resetCamera() {
        resetCamera(Display.getCamera(), viewpointModel);
        render(1);
    }

    public static JSONObject cameraToJson() {
        return Display.getCamera().toJson();
    }

    public static void cameraFromJson(JSONObject json) {
        Display.getCamera().fromJson(json, viewpointModel.getViewpoint());
    }

    static void resetCameras() {
        resetCamera(Display.getMiniCamera(), miniViewpointModel);
        resetCamera(Display.getCamera(), viewpointModel);
        render(1);
    }

    public static void zoomMiniToFit() {
        ViewActions.fitCameraToImageLayers(Display.getMiniCamera(), miniViewpointModel.getViewpoint());
    }

    private static void resetCamera(Camera camera, ViewpointModel model) {
        Position viewpoint = model.update(Movie.getTime());
        camera.reset(viewpoint);
        ViewActions.fitCameraToImageLayers(camera, viewpoint);
    }

    public static void render(float decodeFactor) {
        Position viewpoint = viewpointModel.getViewpoint();
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor, viewpoint);
        else
            display(viewpoint);
    }

    public static void display() {
        display(viewpointModel.getViewpoint());
    }

    public static void display(Position viewpoint) {
        renderRequestHandler.accept(viewpoint);
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
