package org.helioviewer.jhv.gui.controller;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;
import org.helioviewer.jhv.opengl.GLInfo;

public class InputController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private static Camera camera;
    private static Component component;

    private boolean buttonDown = false;

    public InputController(Camera _camera, Component _component) {
        camera = _camera;
        component = _component;
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
        component.addKeyListener(this);
    }

    private MouseEvent synthesizeMouse(MouseEvent e) {
        return new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                              e.getX() * GLInfo.pixelScale[0], e.getY() * GLInfo.pixelScale[1],
                              e.getClickCount(), e.isPopupTrigger(), e.getButton());
    }

    private MouseWheelEvent synthesizeMouseWheel(MouseWheelEvent e) {
        return new MouseWheelEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                                   e.getX() * GLInfo.pixelScale[0], e.getY() * GLInfo.pixelScale[1],
                                   e.getClickCount(),  e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e.getWheelRotation());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        e = synthesizeMouse(e);

        ImageViewerGui.getCurrentInteraction().mouseClicked(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseClicked(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        e = synthesizeMouse(e);

        if (ImageViewerGui.getCurrentInteraction() != ImageViewerGui.getAnnotateInteraction()) {
            component.setCursor(buttonDown ? UIGlobals.closedHandCursor : UIGlobals.openHandCursor);
        }

        for (MouseListener listener : mouseListeners)
            listener.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        e = synthesizeMouse(e);

        component.setCursor(Cursor.getDefaultCursor());

        for (MouseListener listener : mouseListeners)
            listener.mouseExited(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        e = synthesizeMouse(e);

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (ImageViewerGui.getCurrentInteraction() != ImageViewerGui.getAnnotateInteraction()) {
                component.setCursor(UIGlobals.closedHandCursor);
            }
            buttonDown = true;
        }

        ImageViewerGui.getCurrentInteraction().mousePressed(e);
        for (MouseListener listener : mouseListeners)
            listener.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        e = synthesizeMouse(e);

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (ImageViewerGui.getCurrentInteraction() != ImageViewerGui.getAnnotateInteraction()) {
                component.setCursor(UIGlobals.openHandCursor);
            }
            buttonDown = false;
        }

        ImageViewerGui.getCurrentInteraction().mouseReleased(e);
        for (MouseListener listener : mouseListeners)
            listener.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        e = synthesizeMouse(e);

        ImageViewerGui.getCurrentInteraction().mouseDragged(e);
        for (MouseMotionListener listener : mouseMotionListeners)
            listener.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        e = synthesizeMouse(e);

        ImageViewerGui.getCurrentInteraction().mouseMoved(e);
        for (MouseMotionListener listener : mouseMotionListeners)
            listener.mouseMoved(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        e = synthesizeMouseWheel(e);

        ImageViewerGui.getCurrentInteraction().mouseWheelMoved(e);
        for (MouseWheelListener listener : mouseWheelListeners)
            listener.mouseWheelMoved(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        ImageViewerGui.getCurrentInteraction().keyTyped(e);
        for (KeyListener listener : keyListeners)
            listener.keyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        ImageViewerGui.getCurrentInteraction().keyPressed(e);
        for (KeyListener listener : keyListeners)
            listener.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        ImageViewerGui.getCurrentInteraction().keyReleased(e);
        for (KeyListener listener : keyListeners)
            listener.keyReleased(e);
    }

    private final HashSet<MouseListener> mouseListeners = new HashSet<MouseListener>();
    private final HashSet<MouseMotionListener> mouseMotionListeners = new HashSet<MouseMotionListener>();
    private final HashSet<MouseWheelListener> mouseWheelListeners = new HashSet<MouseWheelListener>();
    private final HashSet<KeyListener> keyListeners = new HashSet<KeyListener>();

    public void addPlugin(InputControllerPlugin plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.add((MouseListener) plugin);
        if (plugin instanceof MouseMotionListener)
            mouseMotionListeners.add((MouseMotionListener) plugin);
        if (plugin instanceof MouseWheelListener)
            mouseWheelListeners.add((MouseWheelListener) plugin);
        if (plugin instanceof KeyListener)
            keyListeners.add((KeyListener) plugin);
        plugin.setCamera(camera);
        plugin.setComponent(component);
    }

    public void removePlugin(InputControllerPlugin plugin) {
        if (plugin instanceof MouseListener)
            mouseListeners.remove((MouseListener) plugin);
        if (plugin instanceof MouseMotionListener)
            mouseMotionListeners.remove((MouseMotionListener) plugin);
        if (plugin instanceof MouseWheelListener)
            mouseWheelListeners.remove((MouseWheelListener) plugin);
        if (plugin instanceof KeyListener)
            keyListeners.remove((KeyListener) plugin);
        plugin.setCamera(null);
        plugin.setComponent(null);
    }

}
