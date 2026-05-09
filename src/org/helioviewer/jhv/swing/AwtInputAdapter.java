package org.helioviewer.jhv.swing;

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
        inputController.mouseClicked(synthesizePointer(e));
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
        inputController.mouseWheelMoved(new ScrollEvent(
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getPreciseWheelRotation()));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        inputController.keyPressed(synthesizeKey(e));
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        inputController.keyReleased(synthesizeKey(e));
    }
}
