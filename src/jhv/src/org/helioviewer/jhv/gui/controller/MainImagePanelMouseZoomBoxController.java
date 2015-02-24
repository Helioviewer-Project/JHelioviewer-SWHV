package org.helioviewer.jhv.gui.controller;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Implementation of ImagePanelInputController for the main image panel using
 * zoom box selection mode.
 *
 * <p>
 * By using this controller, the user can zoom in the region of interest of the
 * main image panel by selecting the desired area with a rubber band. Zooming is
 * also possible by using the mouse wheel or double-clicking into the image.
 * After selecting an area, the selection mode switches back to pan selection
 * mode.
 *
 * <p>
 * Also, see {@link org.helioviewer.jhv.gui.components.MainImagePanel} and
 * {@link MainImagePanelMousePanController}.
 *
 */
public class MainImagePanelMouseZoomBoxController extends MainImagePanelMouseController {

    private Vector2dInt lastMouseCoordinates;
    private Vector2dInt draggedToCoordinates;
    private final Rectangle rectangle = new Rectangle();

    private final Rubberband rubberband = new Rubberband();

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View newView) {
        super.setView(newView);

        if (newView != null)
            newView.getAdapter(ComponentView.class).addPostRenderer(rubberband);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (imagePanel != null) {
            imagePanel.setCursor(Cursor.getDefaultCursor());
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
        if (e.getButton() == MouseEvent.BUTTON1) {
            lastMouseCoordinates = new Vector2dInt(e.getX(), e.getY());
            rectangle.x = e.getX();
            rectangle.y = e.getY();
            rectangle.width = 0;
            rectangle.height = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            draggedToCoordinates = new Vector2dInt(e.getX(), e.getY());

            Region r = regionView.getRegion();
            Viewport v = viewportView.getViewport();
            MetaData m = metaDataView.getMetaData();
            if (r == null || v == null || m == null) {
                return;
            }

            recalculateRectangle();

            if (rectangle.height <= 2 && rectangle.width <= 2)
                return;

            ViewportImageSize vis = ViewHelper.calculateViewportImageSize(v, r);

            Vector2dDouble imageOffset = ViewHelper.convertScreenToImageDisplacement(new Vector2dInt(rectangle.x, rectangle.y + rectangle.height).subtract(new Vector2dInt(0, v.getHeight())), r, vis);
            Vector2dDouble newCorner = Vector2dDouble.add(r.getLowerLeftCorner(), imageOffset);

            Vector2dDouble size = ViewHelper.convertScreenToImageDisplacement(new Vector2dInt(rectangle.width, rectangle.height * -1), r, vis);

            Region region = StaticRegion.createAdaptedRegion(newCorner, size);
            Region regionShifted = ViewHelper.cropRegionToImage(region, m);
            regionView.setRegion(regionShifted, new ChangeEvent());

            lastMouseCoordinates = null;

            // ImageViewerGui.getSingletonInstance().getTopToolBar().setActiveSelectionMode(SelectionMode.PAN);
            ImageViewerGui.getSingletonInstance().getMainImagePanel().getInputController().mouseEntered(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMouseCoordinates != null) {
            draggedToCoordinates = new Vector2dInt(e.getX(), e.getY());

            recalculateRectangle();
            imagePanel.repaint();
        }
    }

    /**
     * Get method that calculates and returns the rectangle of the rubber band.
     *
     */
    private void recalculateRectangle() {

        if (lastMouseCoordinates.getX() < draggedToCoordinates.getX()) {
            rectangle.x = lastMouseCoordinates.getX();
            rectangle.width = draggedToCoordinates.getX() - lastMouseCoordinates.getX();
        } else {
            rectangle.x = draggedToCoordinates.getX();
            rectangle.width = lastMouseCoordinates.getX() - draggedToCoordinates.getX();
        }

        if (lastMouseCoordinates.getY() < draggedToCoordinates.getY()) {
            rectangle.y = lastMouseCoordinates.getY();
            rectangle.height = draggedToCoordinates.getY() - lastMouseCoordinates.getY();
        } else {
            rectangle.y = draggedToCoordinates.getY();
            rectangle.height = lastMouseCoordinates.getY() - draggedToCoordinates.getY();
        }
    }

    /**
     * Private class implementing ScreenRenderer to draw the rubber band onto
     * the image.
     *
     * <p>
     * For further informations about drawing into the image, see
     * {@link org.helioviewer.viewmodel.renderer.screen.ScreenRenderer}.
     */
    private class Rubberband implements ScreenRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public void render(ScreenRenderGraphics g) {
            if (lastMouseCoordinates != null) {
                g.setColor(Color.YELLOW);
                g.drawRectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
        }
    }
}
