package org.helioviewer.jhv.display;

import java.util.function.Consumer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageBounds;

import org.json.JSONObject;

public final class DisplayController {

    private DisplayController() {}

    public enum ViewpointApplyMode {
        RESET,
        KEEP_TRANSFORM
    }

    private static final ViewpointModel viewpointModel = new ViewpointModel(UpdateViewpoint.observer);
    private static final ViewpointModel miniViewpointModel = new ViewpointModel(UpdateViewpoint.earthAt1au);

    private static boolean missingHandlerLogged;
    private static Consumer<Position> renderRequestHandler = _ -> missingRenderRequestHandler();

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

    public static UpdateViewpoint getViewpointUpdate() {
        return viewpointModel.getUpdateViewpoint();
    }

    public static boolean getTrackingMode() {
        return viewpointModel.getTrackingMode();
    }

    public static void addViewpointListener(ViewpointListener listener) {
        viewpointModel.addListener(listener);
    }

    public static void removeViewpointListener(ViewpointListener listener) {
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

    public static void timeChanged(JHVTime time) {
        if (!viewpointModel.getTrackingMode())
            updateViewpoint(time);
    }

    public static void refreshCamera() {
        updateViewpoint(Movie.getTime());
        render(1);
    }

    private static void updateViewpoint(JHVTime time) {
        Display.getCamera().updateViewpoint(viewpointModel.update(time));
    }

    public static void resetCamera() {
        resetCamera(Display.getCamera(), viewpointModel);
        render(1);
    }

    static void resetCameras() {
        resetCamera(Display.getMiniCamera(), miniViewpointModel);
        resetCamera(Display.getCamera(), viewpointModel);
        render(1);
    }

    private static void resetCamera(Camera camera, ViewpointModel model) {
        Position viewpoint = model.update(Movie.getTime());
        camera.reset(viewpoint);
        fitCameraToImageLayers(camera, viewpoint);
    }

    public static void zoomFit() {
        Display.resetViewportZoom();
        fitCameraToImageLayers(Display.getCamera(), GLRenderer.getDisplayedViewpoint());
        render(1);
    }

    public static void zoomMiniToFit() {
        fitCameraToImageLayers(Display.getMiniCamera(), miniViewpointModel.getViewpoint());
    }

    private static void fitCameraToImageLayers(Camera camera, Position viewpoint) {
        double size = Display.mode == ProjectionMode.Orthographic ? ImageLayers.getLargestPhysicalHeight() : 1;
        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, viewpoint.distance);
        camera.setFOV(newFOV, viewpoint);
    }

    public static void zoomIn() {
        zoomViewports(-Camera.ZOOM_MULTIPLIER_BUTTON);
        render(1);
    }

    public static void zoomOut() {
        zoomViewports(+Camera.ZOOM_MULTIPLIER_BUTTON);
        display();
    }

    private static void zoomViewports(double wr) {
        double factor = Camera.zoomFactor(wr);
        for (Viewport viewport : Display.getViewports())
            viewport.zoom *= factor;
    }

    public static void zoomOneToOne() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null)
            return;

        Camera camera = Display.getCamera();
        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        double cameraWidth = oneToOneCameraWidth(layer, Display.getActiveViewport(), Display.mode);
        if (cameraWidth > 0) {
            Display.resetViewportZoom();
            double fov = 2. * Math.atan2(0.5 * cameraWidth, viewpoint.distance);
            camera.setFOV(fov, viewpoint);
        }
        render(1);
    }

    private static double oneToOneCameraWidth(ImageLayer layer, Viewport vp, ProjectionMode mode) {
        ImageData imageData = layer.getImageData();
        if (imageData == null)
            return 0;

        MetaData metaData = imageData.getMetaData();
        double imageHeight = oneToOneImageHeight(metaData, mode);
        double cameraWidth = vp.height * metaData.getUnitPerPixelY() * imageHeight / metaData.getPhysicalRegion().height;
        if (mode == ProjectionMode.Orthographic)
            return cameraWidth;

        double viewportHeight = oneToOneViewportHeight(vp, mode);
        return viewportHeight > 0 ? cameraWidth / viewportHeight : 0;
    }

    private static double oneToOneImageHeight(MetaData metaData, ProjectionMode mode) {
        return mode == ProjectionMode.HPC ? ImageBounds.hpc(metaData).height : metaData.getPhysicalRegion().height;
    }

    private static double oneToOneViewportHeight(Viewport vp, ProjectionMode mode) {
        if (mode == ProjectionMode.HPC) {
            Region bounds = ImageLayers.computeHpcScaleBounds();
            double halfWidth = 0.5 * bounds.width;
            double halfHeight = 0.5 * bounds.height;
            halfHeight = Math.max(halfHeight, halfWidth / vp.aspect);
            return 2 * halfHeight;
        }
        return 1;
    }

    public static void resetView() {
        Display.resetViewportZoom();
        resetCamera();
    }

    public static void resetViewAxis() {
        Display.getCamera().resetDragRotationAxis(getViewpointUpdate().dragAxis());
        display();
    }

    public static void rotateView90(String axis) {
        if (axis == null)
            return;

        switch (axis.toUpperCase()) {
            case "X" -> rotateView90(Quat.X90);
            case "Y" -> rotateView90(Quat.Y90);
            case "Z" -> rotateView90(Quat.Z90);
            default -> Log.warn("Ignoring invalid rotate view axis value: " + axis);
        }
    }

    private static void rotateView90(Quat rotation) {
        Display.getCamera().rotateDragRotation(rotation);
        display();
    }

    public static JSONObject cameraToJson() {
        return Display.getCamera().toJson();
    }

    public static void cameraFromJson(JSONObject json) {
        Display.getCamera().fromJson(json, viewpointModel.getViewpoint());
    }
}
