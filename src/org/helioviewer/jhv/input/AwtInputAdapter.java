package org.helioviewer.jhv.input;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.helioviewer.jhv.display.Display;

public final class AwtInputAdapter implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private final InputController inputController;

    public AwtInputAdapter(InputController _inputController) {
        inputController = _inputController;
    }

    private static PointerEvent synthesizePointer(MouseEvent e) {
        return new PointerEvent(
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getButton(),
                e.getClickCount(),
                e.isShiftDown(),
                e.isPopupTrigger());
    }

    private static ScrollEvent synthesizeScroll(MouseWheelEvent e) {
        return new ScrollEvent(e.getPreciseWheelRotation());
    }

    private static KeyInputEvent synthesizeKey(KeyEvent e) {
        return new KeyInputEvent(e.getKeyCode(), e.isShiftDown());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        inputController.mouseClicked(synthesizePointer(e));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        inputController.mouseEntered(synthesizePointer(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        inputController.mouseExited(synthesizePointer(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        ((Component) e.getSource()).requestFocusInWindow();
        inputController.mousePressed(synthesizePointer(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        inputController.mouseReleased(synthesizePointer(e));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        inputController.mouseDragged(synthesizePointer(e));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        inputController.mouseMoved(synthesizePointer(e));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        inputController.mouseWheelMoved(synthesizeScroll(e));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        inputController.keyPressed(synthesizeKey(e));
    }

    @Override
    public void keyTyped(KeyEvent e) {
        inputController.keyTyped(synthesizeKey(e));
    }

    @Override
    public void keyReleased(KeyEvent e) {
        inputController.keyReleased(synthesizeKey(e));
    }
}
