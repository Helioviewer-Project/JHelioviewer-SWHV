package org.helioviewer.jhv.camera;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;

public final class ViewActions {

    private ViewActions() {
    }

    public static void zoomFit() {
        CameraHelper.zoomToFit(Display.getCamera());
        MovieDisplay.render(1);
    }

    public static void zoomIn() {
        Display.getCamera().zoom(-Camera.ZOOM_MULTIPLIER_BUTTON);
        MovieDisplay.render(1);
    }

    public static void zoomOut() {
        Display.getCamera().zoom(+Camera.ZOOM_MULTIPLIER_BUTTON);
        MovieDisplay.display();
    }

    public static void zoomOneToOne() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null)
            return;

        Camera camera = Display.getCamera();
        double cameraWidth = ImageLayerBounds.getOneToOneCameraWidth(layer);
        if (cameraWidth > 0) {
            double fov = 2. * Math.atan2(0.5 * cameraWidth, camera.getViewpoint().distance);
            camera.setFOV(fov);
        }
        MovieDisplay.render(1);
    }

    public static void resetView() {
        Display.getCamera().reset();
    }

    public static void resetViewAxis() {
        Display.getCamera().resetDragRotationAxis();
        MovieDisplay.display();
    }

    public static void rotateView90(@Nullable Quat rotation) {
        if (rotation == null)
            return;
        Display.getCamera().rotateDragRotation(rotation);
        MovieDisplay.display();
    }
}
