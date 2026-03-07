package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Viewport;
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
    public void mousePressed(MouseEvent e, Viewport vp) {
        lastX = e.getX();
        lastY = e.getY();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(MouseEvent e, Viewport vp) {
        if (!dragStartSet)
            return;

        int x = e.getX() - lastX;
        int y = e.getY() - lastY;
        lastX = e.getX();
        lastY = e.getY();

        double m = 1 / CameraHelper.getImagePixelFactor(camera, vp);
        camera.setTranslation(camera.getTranslationX() + x * m, camera.getTranslationY() - y * m);
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartSet = false;
    }

}
