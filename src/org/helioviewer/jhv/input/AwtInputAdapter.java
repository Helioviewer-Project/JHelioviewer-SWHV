package org.helioviewer.jhv.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public final class AwtInputAdapter implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private final InputController inputController;

    public AwtInputAdapter(InputController _inputController) {
        inputController = _inputController;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        inputController.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        inputController.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        inputController.mouseExited(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        inputController.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        inputController.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        inputController.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        inputController.mouseMoved(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        inputController.mouseWheelMoved(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        inputController.keyPressed(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        inputController.keyTyped(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        inputController.keyReleased(e);
    }
}
