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
import java.util.LinkedList;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;

public class InputController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private static Component component;

    private boolean buttonDown = false;
    private long lastTime = System.currentTimeMillis();

    public InputController(Component _component) {
        component = _component;
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
        component.addKeyListener(this);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        GL3DCamera camera = Displayer.getViewport().getCamera();
        if (camera.getCurrentInteraction() != camera.getAnnotateInteraction()) {
            component.setCursor(buttonDown ? UIGlobals.closedHandCursor : UIGlobals.openHandCursor);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        component.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GL3DCamera camera = Displayer.getViewport().getCamera();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (camera.getCurrentInteraction() != camera.getAnnotateInteraction()) {
                component.setCursor(UIGlobals.closedHandCursor);
            }
            buttonDown = true;
        }
        camera.getCurrentInteraction().mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            component.setCursor(UIGlobals.openHandCursor);
            buttonDown = false;
        }
        Displayer.getViewport().getCamera().getCurrentInteraction().mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (buttonDown && currentTime - lastTime > 30) {
            lastTime = currentTime;
            Displayer.getViewport().getCamera().getCurrentInteraction().mouseDragged(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().mouseClicked(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().mouseWheelMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().mouseMoved(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().keyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Displayer.getViewport().getCamera().getCurrentInteraction().keyReleased(e);
    }

    private final LinkedList<InputControllerPlugin> plugins = new LinkedList<InputControllerPlugin>();

    public void addPlugin(InputControllerPlugin newPlugin) {
        if (newPlugin == null || plugins.contains(newPlugin)) {
            return;
        }

        plugins.add(newPlugin);
        newPlugin.setComponent(component);

        if (newPlugin instanceof MouseListener)
            component.addMouseListener((MouseListener) newPlugin);
        if (newPlugin instanceof MouseMotionListener)
            component.addMouseMotionListener((MouseMotionListener) newPlugin);
        if (newPlugin instanceof MouseWheelListener)
            component.addMouseWheelListener((MouseWheelListener) newPlugin);
        if (newPlugin instanceof KeyListener)
            component.addKeyListener((KeyListener) newPlugin);
    }

    public void removePlugin(InputControllerPlugin oldPlugin) {
        if (oldPlugin == null || !plugins.remove(oldPlugin)) {
            return;
        }

        oldPlugin.setComponent(null);

        if (oldPlugin instanceof MouseListener)
            component.removeMouseListener((MouseListener) oldPlugin);
        if (oldPlugin instanceof MouseMotionListener)
            component.removeMouseMotionListener((MouseMotionListener) oldPlugin);
        if (oldPlugin instanceof MouseWheelListener)
            component.removeMouseWheelListener((MouseWheelListener) oldPlugin);
        if (oldPlugin instanceof KeyListener)
            component.removeKeyListener((KeyListener) oldPlugin);
    }

}
