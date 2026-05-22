package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayFrame;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;

public final class ViewActions {

    private ViewActions() {}

    public static void zoomFit() {
        Display.resetViewportZoom();
        CameraHelper.zoomToFit(Display.getCamera(), DisplayFrame.getViewpoint());
        MovieDisplay.render(1);
    }

    public static void zoomIn() {
        zoomViewports(-Camera.ZOOM_MULTIPLIER_BUTTON);
        MovieDisplay.render(1);
    }

    public static void zoomOut() {
        zoomViewports(+Camera.ZOOM_MULTIPLIER_BUTTON);
        MovieDisplay.display();
    }

    public static void zoomOneToOne() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null)
            return;

        Camera camera = Display.getCamera();
        double cameraWidth = ImageLayerBounds.getOneToOneCameraWidth(layer);
        if (cameraWidth > 0) {
            Display.resetViewportZoom();
            Position viewpoint = DisplayFrame.getViewpoint();
            double fov = 2. * Math.atan2(0.5 * cameraWidth, viewpoint.distance);
            camera.setFOV(fov, viewpoint);
        }
        MovieDisplay.render(1);
    }

    public static void resetView() {
        Display.resetViewportZoom();
        DisplayFrame.resetCamera();
    }

    public static void resetViewAxis() {
        Display.getCamera().resetDragRotationAxis(DisplayFrame.getViewpointUpdate().dragAxis());
        MovieDisplay.display();
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
        MovieDisplay.display();
    }

    private static void zoomViewports(double wr) {
        double factor = Camera.zoomFactor(wr);
        for (Viewport viewport : Display.getViewports())
            viewport.zoom *= factor;
    }
}
