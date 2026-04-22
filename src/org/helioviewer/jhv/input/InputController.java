package org.helioviewer.jhv.input;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;

public class InputController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final Interaction interaction;

    public InputController(Interaction _interaction) {
        interaction = _interaction;
    }

    private static PointerEvent synthesizePointer(MouseEvent e) {
        return new PointerEvent(
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getButton(),
                e.getClickCount(),
                e.isShiftDown());
    }

    private static ScrollEvent synthesizeScroll(MouseWheelEvent e) {
        return new ScrollEvent(e.getPreciseWheelRotation());
    }

    private static KeyInputEvent synthesizeKey(KeyEvent e) {
        return new KeyInputEvent(e.getKeyCode(), e.isShiftDown());
    }

    private static MouseEvent synthesizeMouse(MouseEvent e) {
        return new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiersEx(),
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getClickCount(), e.isPopupTrigger(), e.getButton());
    }

    /* Could be useful if pointer position would matter
    private static MouseWheelEvent synthesizeMouseWheel(MouseWheelEvent e) {
        return new MouseWheelEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiersEx(),
                (int) (e.getX() * Display.pixelScale[0] + .5),
                (int) (e.getY() * Display.pixelScale[1] + .5),
                e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e.getWheelRotation());
    }
    */

    @Override
    public void mouseClicked(MouseEvent e1) {
        PointerEvent e = synthesizePointer(e1);
        MouseEvent mouse = synthesizeMouse(e1);
        Display.setActiveViewport(e.x(), e.y());
        interaction.mouseClicked(e);
        mouseListeners.forEach(listener -> listener.mouseClicked(mouse));
    }

    @Override
    public void mouseEntered(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        mouseListeners.forEach(listener -> listener.mouseEntered(e));
    }

    @Override
    public void mouseExited(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        mouseListeners.forEach(listener -> listener.mouseExited(e));
    }

    @Override
    public void mousePressed(MouseEvent e1) {
        PointerEvent e = synthesizePointer(e1);
        MouseEvent mouse = synthesizeMouse(e1);
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mousePressed(e, vp);
        mouseListeners.forEach(listener -> listener.mousePressed(mouse));
    }

    @Override
    public void mouseReleased(MouseEvent e1) {
        PointerEvent e = synthesizePointer(e1);
        MouseEvent mouse = synthesizeMouse(e1);
        interaction.mouseReleased(e);
        mouseListeners.forEach(listener -> listener.mouseReleased(mouse));
    }

    @Override
    public void mouseDragged(MouseEvent e1) {
        PointerEvent e = synthesizePointer(e1);
        MouseEvent mouse = synthesizeMouse(e1);
        Viewport vp = Display.setActiveViewport(e.x(), e.y());
        interaction.mouseDragged(e, vp);
        mouseMotionListeners.forEach(listener -> listener.mouseDragged(mouse));
    }

    @Override
    public void mouseMoved(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        Display.setActiveViewport(e.getX(), e.getY());
        mouseMotionListeners.forEach(listener -> listener.mouseMoved(e));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e1) {
        // MouseWheelEvent e = synthesizeMouseWheel(e1);
        // Display.setActiveViewport(e.getX(), e.getY());
        interaction.mouseWheelMoved(synthesizeScroll(e1));
        mouseWheelListeners.forEach(listener -> listener.mouseWheelMoved(e1));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        interaction.keyPressed(synthesizeKey(e));
        keyListeners.forEach(listener -> listener.keyPressed(e));
    }

    @Override
    public void keyTyped(KeyEvent e) {
        keyListeners.forEach(listener -> listener.keyTyped(e));
    }

    @Override
    public void keyReleased(KeyEvent e) {
        interaction.keyReleased(synthesizeKey(e));
        keyListeners.forEach(listener -> listener.keyReleased(e));
    }

    private final HashSet<MouseListener> mouseListeners = new HashSet<>();
    private final HashSet<MouseMotionListener> mouseMotionListeners = new HashSet<>();
    private final HashSet<MouseWheelListener> mouseWheelListeners = new HashSet<>();
    private final HashSet<KeyListener> keyListeners = new HashSet<>();

    public void addPlugin(Object plugin) {
        if (plugin instanceof MouseListener ml)
            mouseListeners.add(ml);
        if (plugin instanceof MouseMotionListener mml)
            mouseMotionListeners.add(mml);
        if (plugin instanceof MouseWheelListener mwl)
            mouseWheelListeners.add(mwl);
        if (plugin instanceof KeyListener kl)
            keyListeners.add(kl);
    }

    public void removePlugin(Object plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.remove(plugin);
        if (plugin instanceof MouseMotionListener)
            mouseMotionListeners.remove(plugin);
        if (plugin instanceof MouseWheelListener)
            mouseWheelListeners.remove(plugin);
        if (plugin instanceof KeyListener)
            keyListeners.remove(plugin);
    }

}
