package org.helioviewer.jhv.gui.controller;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.MainImagePanel;

/**
 * Acts as the global Delegate for Mouse. Mouse Events are
 * delegated to the {@link GL3DCamera}
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class CameraMouseController implements MouseInputListener, MouseWheelListener {

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    private static MainImagePanel imagePanel;

    private boolean buttonDown = false;
    private long lastTime = System.currentTimeMillis();

    public void setImagePanel(MainImagePanel newImagePanel) {
        imagePanel = newImagePanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (imagePanel != null) {
            GL3DCamera camera = GL3DState.getActiveCamera();
            if (camera.getCurrentInteraction() == camera.getZoomInteraction()) {
            } else {
                imagePanel.setCursor(buttonDown ? closedHandCursor : openHandCursor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
        if (imagePanel != null) {
            imagePanel.setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        GL3DCamera camera = GL3DState.getActiveCamera();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (camera.getCurrentInteraction() != camera.getZoomInteraction()) {
                imagePanel.setCursor(closedHandCursor);
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
            imagePanel.setCursor(openHandCursor);
            buttonDown = false;
        }
        GL3DState.getActiveCamera().getCurrentInteraction().mouseReleased(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (buttonDown && currentTime - lastTime > 30) {
            lastTime = currentTime;
            GL3DState.getActiveCamera().getCurrentInteraction().mouseDragged(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        GL3DState.getActiveCamera().getCurrentInteraction().mouseClicked(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        GL3DState.getActiveCamera().getCurrentInteraction().mouseWheelMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        GL3DState.getActiveCamera().getCurrentInteraction().mouseMoved(e);
    }

}
