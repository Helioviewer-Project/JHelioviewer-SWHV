package org.helioviewer.gl3d;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.controller.AbstractImagePanelMouseController;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * Acts as the global Delegate for Mouse. Mouse Events are
 * delegated to the {@link GL3DCamera}
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DCameraMouseController extends AbstractImagePanelMouseController {

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    private boolean buttonDown = false;
    private long lastTime = System.currentTimeMillis();

    @Override
    public void setView(ComponentView newView) {
        super.setView(newView);
    }

    private GL3DCamera getCamera() {
        return GL3DState.getActiveCamera();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (imagePanel != null) {
            GL3DCamera camera = getCamera();
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
        super.mouseExited(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (currentCamera.getCurrentInteraction() != currentCamera.getZoomInteraction()) {
                imagePanel.setCursor(closedHandCursor);
            }
            buttonDown = true;
        }
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mousePressed(e);
        }
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
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseReleased(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (buttonDown && currentTime - lastTime > 30) {
            lastTime = currentTime;

            GL3DCamera currentCamera = getCamera();
            if (currentCamera != null) {
                currentCamera.getCurrentInteraction().mouseDragged(e);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseClicked(e);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseWheelMoved(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseMoved(e);
        }
    }

}
