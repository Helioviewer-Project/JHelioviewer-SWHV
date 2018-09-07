package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.display.Display;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class Interaction implements MouseListener, KeyListener {

    final Camera camera;

    Interaction(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        float r = e.getRotation()[1];
        if (r != 0) {
            camera.zoom(-Display.CAMERA_ZOOM_MULTIPLIER_WHEEL * r);
            Display.render();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            camera.reset();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}
