package org.helioviewer.jhv.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.input.ScrollEvent;

public final class AwtInputAdapter extends MouseAdapter implements KeyListener {

    private static PointerEvent synthesizePointer(MouseEvent e) {
        return new PointerEvent(
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getButton(),
                e.getClickCount(),
                e.isShiftDown(),
                e.isPopupTrigger());
    }

    public static Point toAwtPoint(PointerEvent e) {
        return new Point((int) (e.x() / Display.pixelScale[0] + .5), (int) (e.y() / Display.pixelScale[1] + .5));
    }

    private static KeyInputEvent synthesizeKey(KeyEvent e) {
        return new KeyInputEvent(switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE -> KeyInputEvent.Key.BACKSPACE;
            case KeyEvent.VK_DELETE -> KeyInputEvent.Key.DELETE;
            case KeyEvent.VK_N -> KeyInputEvent.Key.N;
            case KeyEvent.VK_P -> KeyInputEvent.Key.P;
            default -> KeyInputEvent.Key.OTHER;
        }, e.isShiftDown());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        InputController.mouseClicked(synthesizePointer(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        InputController.mouseExited(synthesizePointer(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        ((Component) e.getSource()).requestFocusInWindow();
        InputController.mousePressed(synthesizePointer(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        InputController.mouseReleased(synthesizePointer(e));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        InputController.mouseDragged(synthesizePointer(e));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        InputController.mouseMoved(synthesizePointer(e));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        InputController.mouseWheelMoved(new ScrollEvent(
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getPreciseWheelRotation()));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        InputController.keyPressed(synthesizeKey(e));
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        InputController.keyReleased(synthesizeKey(e));
    }
}
