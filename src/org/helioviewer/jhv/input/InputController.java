package org.helioviewer.jhv.input;

import java.util.HashSet;

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
        for (MouseListener listener : mouseListeners)
            listener.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseEntered(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseExited(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseExited(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mousePressed(e);
        for (MouseListener listener : mouseListeners)
            listener.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseReleased(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseDragged(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Display.setActiveViewport(e.getX(), e.getY());

        JHVFrame.getCurrentInteraction().mouseMoved(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseMoved(e);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        JHVFrame.getCurrentInteraction().mouseWheelMoved(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseWheelMoved(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        JHVFrame.getCurrentInteraction().keyPressed(e);
        for (KeyListener listener : keyListeners)
            listener.keyPressed(e);
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
        for (KeyListener listener : keyListeners)
            listener.keyReleased(e);
    }

    private final HashSet<MouseListener> mouseListeners = new HashSet<>();
    private final HashSet<KeyListener> keyListeners = new HashSet<>();

    public void addPlugin(Object plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.add((MouseListener) plugin);
        if (plugin instanceof KeyListener)
            keyListeners.add((KeyListener) plugin);
    }

    public void removePlugin(Object plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.remove(plugin);
        if (plugin instanceof KeyListener)
            keyListeners.remove(plugin);
    }

}
