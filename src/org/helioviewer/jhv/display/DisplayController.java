package org.helioviewer.jhv.display;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.layers.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageBounds;
import org.helioviewer.jhv.wcs.ImageGeometry;

import org.json.JSONObject;

public final class DisplayController {

    public enum ViewpointApplyMode {
        RESET,
        KEEP_TRANSFORM
    }

    private static UpdateViewpoint viewpointUpdate = UpdateViewpoint.observer;
    private static Position mainViewpoint = Sun.StartEarth;
    private static boolean tracking;
    private static Position miniViewpoint = Sun.StartEarth;

    @Nullable
    private static Consumer<Position> renderRequestHandler;
    // Latest request made before a handler was installed; replayed on installation.
    @Nullable
    private static Position pendingViewpoint;

    public static void render() {
        Position viewpoint = mainViewpoint;
        if (!ImageLayers.decode(viewpoint)) // just display() if none enabled
            display(viewpoint);
    }

    public static double getImagePixelFactor(Viewport vp) {
        return ViewportMath.getImagePixelFactor(Display.getCamera(), vp);
    }

    public static void display() {
        display(mainViewpoint);
    }

    public static void display(Position viewpoint) {
        if (renderRequestHandler == null)
            pendingViewpoint = viewpoint;
        else
            renderRequestHandler.accept(viewpoint);
    }

    public static void setRenderRequestHandler(Consumer<Position> _renderRequestHandler) {
        renderRequestHandler = _renderRequestHandler;
        Position viewpoint = pendingViewpoint;
        if (viewpoint != null) {
            pendingViewpoint = null;
            renderRequestHandler.accept(viewpoint);
        }
    }

    public static UpdateViewpoint getViewpointUpdate() {
        return viewpointUpdate;
    }

    public static boolean getTrackingMode() {
        return tracking;
    }

    public static void setViewpointUpdate(UpdateViewpoint updateViewpoint, ViewpointApplyMode mode) {
        viewpointUpdate = updateViewpoint;
        switch (mode) {
            case RESET -> resetCamera();
            case KEEP_TRANSFORM -> updateViewpoint(Player.getTime());
        }
    }

    public static void setTrackingMode(boolean _tracking) {
        if (tracking == _tracking)
            return;
        tracking = _tracking;
        refreshCamera();
    }

    public static void timeChanged(JHVTime time) {
        if (!tracking)
            updateViewpoint(time);
    }

    public static void refreshCamera() {
        updateViewpoint(Player.getTime());
        render();
    }

    private static void updateViewpoint(JHVTime time) {
        mainViewpoint = viewpointUpdate.update(time);
        Display.getCamera().updateViewpoint(mainViewpoint);
    }

    public static void resetCamera() {
        mainViewpoint = viewpointUpdate.update(Player.getTime());
        resetCamera(Display.getCamera(), mainViewpoint);
        render();
    }

    static void resetCameras() {
        miniViewpoint = UpdateViewpoint.earthAt1au.update(Player.getTime());
        resetCamera(Display.getMiniCamera(), miniViewpoint);
        resetCamera();
    }

    private static void resetCamera(Camera camera, Position viewpoint) {
        camera.reset(viewpoint);
        fitCameraToImageLayers(camera, viewpoint);
    }

    public static void zoomFit() {
        Display.resetViewportZoom();
        fitCameraToImageLayers(Display.getCamera(), GLRenderer.getDisplayedViewpoint());
        render();
    }

    public static void zoomMiniToFit() {
        fitCameraToImageLayers(Display.getMiniCamera(), miniViewpoint);
    }

    public static void zoomToFovAnnotation(double centerX, double centerY, double halfSize) {
        Display.resetViewportZoom();
        Camera camera = Display.getCamera();
        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        camera.setTranslation(-centerX, -centerY);
        camera.resetDragRotation();
        camera.setFOV(2 * Math.atan2(halfSize, viewpoint.distance), viewpoint);
    }

    private static void fitCameraToImageLayers(Camera camera, Position viewpoint) {
        double size = Display.mode == MapMode.Orthographic ? ImageLayers.getLargestPhysicalHeight() : 1;
        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, viewpoint.distance);
        camera.setFOV(newFOV, viewpoint);
    }

    public static void zoomIn() {
        zoomViewports(-Camera.ZOOM_MULTIPLIER_BUTTON);
        render();
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
        Camera camera = Display.getCamera();
        Position viewpoint = GLRenderer.getDisplayedViewpoint();
        double cameraWidth = oneToOneCameraWidth(layer, Display.getActiveViewport(), Display.mode, viewpoint);
        if (cameraWidth > 0) {
            Display.resetViewportZoom();
            double fov = 2. * Math.atan2(0.5 * cameraWidth, viewpoint.distance);
            camera.setFOV(fov, viewpoint);
        }
        render();
    }

    private static double oneToOneCameraWidth(ImageLayer layer, Viewport vp, MapMode mode, Position viewpoint) {
        ImageData imageData = layer.getImageData();
        if (imageData == null)
            return 0;

        ImageGeometry geometry = imageData.metaData().getGeometry();
        if (mode != MapMode.HPC) // one viewport pixel per image pixel
            return vp.height * geometry.pixelSizeY;

        Region scale = ImageLayers.computeHpcScaleBounds(viewpoint);
        double viewportHeight = Math.max(scale.height, scale.width / vp.aspect);
        double distance = geometry.tracingViewpoint(viewpoint, imageData.metaData().getViewpoint()).distance;
        return viewportHeight > 0 ? vp.height * ImageBounds.hpc(geometry, distance).height
                / (geometry.grid.height() * viewportHeight) : 0;
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
        Display.getCamera().fromJson(json, mainViewpoint);
    }

    private DisplayController() {}
}
