package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Vec2;

class InteractionPan implements Interaction.Type {

    private final Camera camera;
    private int lastX;
    private int lastY;
    private boolean dragStartSet; // avoid freak mouseDragged before mousePressed

    InteractionPan(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastX = e.getX();
        lastY = e.getY();
        dragStartSet = true;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragStartSet)
            return;

        int x = e.getX() - lastX;
        int y = e.getY() - lastY;
        lastX = e.getX();
        lastY = e.getY();

        Vec2 pan = camera.getTranslation();
        double m = 1 / CameraHelper.getImagePixelFactor(camera, Display.getActiveViewport());
        camera.setTranslation(pan.x + x * m, pan.y - y * m);
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartSet = false;
    }

}
