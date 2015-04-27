package org.helioviewer.jhv.gui.controller;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Acts as the global Delegate for Mouse. Mouse Events are
 * delegated to the {@link GL3DCamera}
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class CameraMouseController implements MouseListener, MouseMotionListener, MouseWheelListener {

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    private static Component component;

    private boolean buttonDown = false;
    private long lastTime = System.currentTimeMillis();

    public CameraMouseController(Component _component) {
        component = _component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        GL3DCamera camera = Displayer.getActiveCamera();
        if (camera.getCurrentInteraction() != camera.getZoomInteraction()) {
            component.setCursor(buttonDown ? closedHandCursor : openHandCursor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
        component.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        GL3DCamera camera = Displayer.getActiveCamera();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (camera.getCurrentInteraction() != camera.getZoomInteraction()) {
                component.setCursor(closedHandCursor);
            }
            buttonDown = true;
        }
        camera.getCurrentInteraction().mousePressed(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            component.setCursor(openHandCursor);
            buttonDown = false;
        }
        Displayer.getActiveCamera().getCurrentInteraction().mouseReleased(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (buttonDown && currentTime - lastTime > 30) {
            lastTime = currentTime;
            Displayer.getActiveCamera().getCurrentInteraction().mouseDragged(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Displayer.getActiveCamera().getCurrentInteraction().mouseClicked(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Displayer.getActiveCamera().getCurrentInteraction().mouseWheelMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Displayer.getActiveCamera().getCurrentInteraction().mouseMoved(e);
    }

}
