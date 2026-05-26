package org.helioviewer.jhv.display;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.GLRenderer;

public final class ViewActions {

    private ViewActions() {}

    public static void zoomFit() {
        Display.resetViewportZoom();
        fitCameraToImageLayers(Display.getCamera(), GLRenderer.getDisplayedViewpoint());
        DisplayController.render(1);
    }

    public static void zoomIn() {
        zoomViewports(-Camera.ZOOM_MULTIPLIER_BUTTON);
        DisplayController.render(1);
    }

    public static void zoomOut() {
        zoomViewports(+Camera.ZOOM_MULTIPLIER_BUTTON);
        DisplayController.display();
    }

    public static void zoomOneToOne() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null)
            return;

        Camera camera = Display.getCamera();
        double cameraWidth = ImageLayerBounds.getOneToOneCameraWidth(layer);
        if (cameraWidth > 0) {
            Display.resetViewportZoom();
            Position viewpoint = GLRenderer.getDisplayedViewpoint();
            double fov = 2. * Math.atan2(0.5 * cameraWidth, viewpoint.distance);
            camera.setFOV(fov, viewpoint);
        }
        DisplayController.render(1);
    }

    public static void resetView() {
        Display.resetViewportZoom();
        DisplayController.resetCamera();
    }

    public static void resetViewAxis() {
        Display.getCamera().resetDragRotationAxis(DisplayController.getViewpointUpdate().dragAxis());
        DisplayController.display();
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
        DisplayController.display();
    }

    static void fitCameraToImageLayers(Camera camera, Position viewpoint) {
        double size = ImageLayerBounds.getLargestPhysicalHeight();
        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, viewpoint.distance);
        camera.setFOV(newFOV, viewpoint);
    }

    private static void zoomViewports(double wr) {
        double factor = Camera.zoomFactor(wr);
        for (Viewport viewport : Display.getViewports())
            viewport.zoom *= factor;
    }
}
