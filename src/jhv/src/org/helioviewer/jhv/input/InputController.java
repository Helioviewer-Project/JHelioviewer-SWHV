package org.helioviewer.jhv.input;

import java.util.HashSet;

import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class InputController implements MouseListener, KeyListener {

    public InputController(Window window) {
        window.addMouseListener(this);
        window.addKeyListener(this);
  }

    @Override
    public void mouseClicked(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseClicked(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseEntered(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseExited(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseExited(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mousePressed(e);
        for (MouseListener listener : mouseListeners)
            listener.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseReleased(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseDragged(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Displayer.setActiveViewport(e.getX(), e.getY());

        ImageViewerGui.getCurrentInteraction().mouseMoved(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseMoved(e);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        ImageViewerGui.getCurrentInteraction().mouseWheelMoved(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseWheelMoved(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        ImageViewerGui.getCurrentInteraction().keyPressed(e);
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

        KeyStroke keyStroke = KeyStroke.getKeyStroke(code, e.getModifiers(), true);
        if (KeyShortcuts.handleKeyStroke(keyStroke, e.getSource(), java.awt.event.KeyEvent.KEY_RELEASED))
            return;

        ImageViewerGui.getCurrentInteraction().keyReleased(e);
        for (KeyListener listener : keyListeners)
            listener.keyReleased(e);
    }

    private final HashSet<MouseListener> mouseListeners = new HashSet<MouseListener>();
    private final HashSet<KeyListener> keyListeners = new HashSet<KeyListener>();

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
