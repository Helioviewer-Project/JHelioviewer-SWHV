package org.helioviewer.jhv.display.interaction;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportProjection;
import org.helioviewer.jhv.input.PointerEvent;

final class InteractionPan extends Interaction.Type {

    private final Camera camera;
    private int lastX;
    private int lastY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionPan(Camera _camera) {
        camera = _camera;
    }

    @Override
    void mousePressed(PointerEvent e, Viewport vp) {
        lastX = e.x();
        lastY = e.y();
        dragStartSet = true;
    }

    @Override
    void mouseDragged(PointerEvent e, Viewport vp) {
        if (!dragStartSet)
            return;

        int x = e.x() - lastX;
        int y = e.y() - lastY;
        lastX = e.x();
        lastY = e.y();

        double m = 1 / ViewportProjection.getImagePixelFactor(camera, vp);
        camera.setTranslation(camera.getTranslationX() + x * m, camera.getTranslationY() - y * m);
        DisplayController.display();
    }

    @Override
    void mouseReleased() {
        dragStartSet = false;
    }

}
