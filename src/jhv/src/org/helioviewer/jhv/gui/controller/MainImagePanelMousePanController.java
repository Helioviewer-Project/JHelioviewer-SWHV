package org.helioviewer.jhv.gui.controller;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Implementation of ImagePanelInputController for the main image panel using
 * pan selection mode.
 * 
 * <p>
 * By using this controller, the user can move the region of interest of the
 * main image panel by dragging the image around. Also, by using the mouse wheel
 * or double-clicking into the image, zooming is possible.
 * 
 * <p>
 * Also, see {@link org.helioviewer.jhv.gui.components.MainImagePanel}.
 * 
 */
public class MainImagePanelMousePanController extends MainImagePanelMouseController {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());

    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    // ///////////////////////////////////////////////////////////////////////////
    // Class variables
    // ///////////////////////////////////////////////////////////////////////////

    private volatile int lastX = -1, lastY = -1;
    private volatile long lastTime = System.currentTimeMillis();

    // ///////////////////////////////////////////////////////////////////////////
    // Mouse events
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
        if (imagePanel != null) {
            imagePanel.setCursor(lastX != -1 ? closedHandCursor : openHandCursor);
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
            imagePanel.setCursor(closedHandCursor);
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            imagePanel.setCursor(openHandCursor);
            lastX = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (lastX != -1 && currentTime - lastTime > 30) {
            lastTime = currentTime;

            Region r = regionView.getRegion();
            Viewport v = viewportView.getViewport();
            MetaData m = metaDataView.getMetaData();
            if (r == null || v == null || m == null) {
                return;
            }

            Vector2dInt screenDisplacement = new Vector2dInt(e.getX() - lastX, e.getY() - lastY);
            Vector2dDouble imageOffset = ViewHelper.convertScreenToImageDisplacement(screenDisplacement, r, ViewHelper.calculateViewportImageSize(v, r));

            Vector2dDouble newCorner = Vector2dDouble.subtract(r.getLowerLeftCorner(), imageOffset);

            Region newRegion = ViewHelper.cropRegionToImage(StaticRegion.createAdaptedRegion(newCorner, r.getSize()), m);

            lastX = e.getX();
            lastY = e.getY();
            regionView.setRegion(newRegion, new ChangeEvent());
        }
    }
}
