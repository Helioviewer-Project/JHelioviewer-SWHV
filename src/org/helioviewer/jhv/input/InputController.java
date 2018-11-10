package org.helioviewer.jhv.input;

import java.util.ArrayList;

import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.JHVFrame;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class InputController implements MouseListener, KeyListener {

    @Override
    public void mouseClicked(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseClicked(e);
        mouseListeners.forEach(listener -> listener.mouseClicked(e));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseEntered(e);
        mouseListeners.forEach(listener -> listener.mouseEntered(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseExited(e);
        mouseListeners.forEach(listener -> listener.mouseExited(e));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mousePressed(e);
        mouseListeners.forEach(listener -> listener.mousePressed(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseReleased(e);
        mouseListeners.forEach(listener -> listener.mouseReleased(e));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseDragged(e);
        mouseListeners.forEach(listener -> listener.mouseDragged(e));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Display.setActiveViewport(e.getX(), e.getY());

        JHVFrame.getCurrentInteraction().mouseMoved(e);
        mouseListeners.forEach(listener -> listener.mouseMoved(e));
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseWheelMoved(e);
        mouseListeners.forEach(listener -> listener.mouseWheelMoved(e));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        JHVFrame.getCurrentInteraction().keyPressed(e);
        keyListeners.forEach(listener -> listener.keyPressed(e));
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

        JHVFrame.getCurrentInteraction().keyReleased(e);
        keyListeners.forEach(listener -> listener.keyReleased(e));
    }

    private final ArrayList<MouseListener> mouseListeners = new ArrayList<>();
    private final ArrayList<KeyListener> keyListeners = new ArrayList<>();

    public void addPlugin(Object plugin) {
        if (plugin instanceof MouseListener && !mouseListeners.contains(plugin))
            mouseListeners.add((MouseListener) plugin);
        if (plugin instanceof KeyListener && !keyListeners.contains(plugin))
            keyListeners.add((KeyListener) plugin);
    }

    public void removePlugin(Object plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.remove(plugin);
        if (plugin instanceof KeyListener)
            keyListeners.remove(plugin);
    }

}
