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

import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.Interaction;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.opengl.GLInfo;

public class InputController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final Interaction interaction;

    public InputController(Interaction _interaction) {
        interaction = _interaction;
    }

    private MouseEvent synthesizeMouse(MouseEvent e) {
        return new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                              (int) (e.getX() * GLInfo.pixelScale[0] + .5),
                              (int) (e.getY() * GLInfo.pixelScale[1] + .5),
                              e.getClickCount(), e.isPopupTrigger(), e.getButton());
    }

    private MouseWheelEvent synthesizeMouseWheel(MouseWheelEvent e) {
        return new MouseWheelEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                                   (int) (e.getX() * GLInfo.pixelScale[0] + .5),
                                   (int) (e.getY() * GLInfo.pixelScale[1] + .5),
                                   e.getClickCount(),  e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e.getWheelRotation());
    }


    @Override
    public void mouseClicked(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mouseClicked(e);
        mouseListeners.forEach(listener -> listener.mouseClicked(e));
    }

    @Override
    public void mouseEntered(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mouseEntered(e);
        mouseListeners.forEach(listener -> listener.mouseEntered(e));
    }

    @Override
    public void mouseExited(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mouseExited(e);
        mouseListeners.forEach(listener -> listener.mouseExited(e));
    }

    @Override
    public void mousePressed(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mousePressed(e);
        mouseListeners.forEach(listener -> listener.mousePressed(e));
    }

    @Override
    public void mouseReleased(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mouseReleased(e);
        mouseListeners.forEach(listener -> listener.mouseReleased(e));
    }

    @Override
    public void mouseDragged(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        interaction.mouseDragged(e);
        mouseMotionListeners.forEach(listener -> listener.mouseDragged(e));
    }

    @Override
    public void mouseMoved(MouseEvent e1) {
        MouseEvent e = synthesizeMouse(e1);
        Display.setActiveViewport(e.getX(), e.getY());
        interaction.mouseMoved(e);
        mouseMotionListeners.forEach(listener -> listener.mouseMoved(e));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e1) {
        MouseWheelEvent e = synthesizeMouseWheel(e1);

        interaction.mouseWheelMoved(e);
        mouseWheelListeners.forEach(listener -> listener.mouseWheelMoved(e));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        interaction.keyPressed(e);
        keyListeners.forEach(listener -> listener.keyPressed(e));
    }

    @Override
    public void keyTyped(KeyEvent e) {
        interaction.keyTyped(e);
        keyListeners.forEach(listener -> listener.keyTyped(e));
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case 0xd:
                code = java.awt.event.KeyEvent.VK_ENTER;
                break;
            case 0x95:
                code = java.awt.event.KeyEvent.VK_LEFT;
                break;
            case 0x97:
                code = java.awt.event.KeyEvent.VK_RIGHT;
                break;
            default:
                break;
        }

        KeyStroke keyStroke = KeyStroke.getKeyStroke(code, e.getModifiers());
        if (KeyShortcuts.handleKeyStroke(keyStroke, e.getSource(), java.awt.event.KeyEvent.KEY_PRESSED))
            return;

        interaction.keyReleased(e);
        keyListeners.forEach(listener -> listener.keyReleased(e));
    }

    private final HashSet<MouseListener> mouseListeners = new HashSet<>();
    private final HashSet<MouseMotionListener> mouseMotionListeners = new HashSet<>();
    private final HashSet<MouseWheelListener> mouseWheelListeners = new HashSet<>();
    private final HashSet<KeyListener> keyListeners = new HashSet<>();

    public void addPlugin(Object plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.add((MouseListener) plugin);
        if (plugin instanceof MouseMotionListener)
            mouseMotionListeners.add((MouseMotionListener) plugin);
        if (plugin instanceof MouseWheelListener)
            mouseWheelListeners.add((MouseWheelListener) plugin);
        if (plugin instanceof KeyListener)
            keyListeners.add((KeyListener) plugin);
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
