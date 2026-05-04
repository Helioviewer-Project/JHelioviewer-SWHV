package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.camera.annotate.AnnotationMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.MovieDisplay;

class InteractionPan implements Interaction.Type {

    private final Camera camera;
    private int lastX;
    private int lastY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionPan(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(PointerEvent e, Viewport vp, AnnotationMode annotationMode) {
        lastX = e.x();
        lastY = e.y();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(PointerEvent e, Viewport vp) {
        if (!dragStartSet)
            return;

        int x = e.x() - lastX;
        int y = e.y() - lastY;
        lastX = e.x();
        lastY = e.y();

        double m = 1 / CameraHelper.getImagePixelFactor(camera, vp);
        camera.setTranslation(camera.getTranslationX() + x * m, camera.getTranslationY() - y * m);
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(PointerEvent e) {
        dragStartSet = false;
    }

}
