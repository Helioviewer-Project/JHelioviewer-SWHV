package org.helioviewer.gl3d.gui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.controller.AbstractImagePanelMouseController;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;

/**
 * Acts as the global Delegate for Mouse and KeyEvents. Mouse Events are
 * delegated to the {@link GL3DCamera} and Key Events to the singleton
 * {@link GL3DKeyController}.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraMouseController extends AbstractImagePanelMouseController implements KeyListener {

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    private boolean buttonDown = false;
    private volatile long lastTime = System.currentTimeMillis();

    private GL3DCameraView cameraView;

    public void setView(View newView) {
        cameraView = ViewHelper.getViewAdapter(newView, GL3DCameraView.class);
        super.setView(newView);
    }

    private GL3DCamera getCamera() {
        if (this.cameraView != null) {
            return this.cameraView.getCurrentCamera();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
        if (imagePanel != null) {
            if (cameraView != null && cameraView.getCurrentCamera().getCurrentInteraction() == cameraView.getCurrentCamera().getZoomInteraction()) {
            } else {
                imagePanel.setCursor(buttonDown ? closedHandCursor : openHandCursor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
        if (imagePanel != null) {
            imagePanel.setCursor(Cursor.getDefaultCursor());
        }
        super.mouseExited(e);
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (cameraView != null && cameraView.getCurrentCamera().getCurrentInteraction() == cameraView.getCurrentCamera().getZoomInteraction()) {
            } else {
                imagePanel.setCursor(closedHandCursor);
            }
            buttonDown = true;
        }
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mousePressed(e);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    public void mouseClicked(MouseEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseClicked(e);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseWheelMoved(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        GL3DCamera currentCamera = getCamera();
        if (currentCamera != null) {
            currentCamera.getCurrentInteraction().mouseMoved(e);
        }
    }

    public void keyPressed(KeyEvent arg0) {
        GL3DKeyController.getInstance().keyPressed(arg0);
    }

    public void keyTyped(KeyEvent arg0) {
        GL3DKeyController.getInstance().keyTyped(arg0);
    }

    public void keyReleased(KeyEvent arg0) {
        GL3DKeyController.getInstance().keyReleased(arg0);
    }

}
