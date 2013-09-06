package org.helioviewer.jhv.gui.controller;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SynchronizeView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Implementation of ImagePanelInputController for the overview image panel.
 * 
 * <p>
 * By using this controller, the user can move the region of interest of the
 * main image panel. This can happen on two ways: Either by dragging the current
 * region or by clicking into the area outside the current region, which will
 * center the region on that point.
 * 
 * <p>
 * Also, see {@link org.helioviewer.jhv.gui.components.OverviewImagePanel}.
 * 
 * @author Stephan Pagel
 * 
 * */
public class OverviewImagePanelMousePanController extends AbstractImagePanelMouseController {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    // Cursor image when mouse button is pressed
    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());

    // Cursor image when cursor is over the image display area
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(9, 9), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    // position and size of ROI rectangle
    private volatile Rectangle roiArea = new Rectangle();
    // position and size of available image data in overview image area
    private volatile Rectangle imageArea = new Rectangle();

    // flag if mouse cursor is inside ROI area
    private volatile boolean insideROI = false;
    // flag if rectangle for ROI was dragged
    private volatile boolean insideROIDragged = false;
    // point where the mouse was last
    private volatile Point lastMouseLocation = new Point(0, 0);

    // Zoom controller
    private volatile ZoomController zoomController = new ZoomController();
    private View observedView;

    private boolean interactionEnabled = false;

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Sets the area where image data is available. (ViewportImageSize + Offset)
     * 
     * @param imageArea
     *            Position and size of the area where image data is available.
     * */
    public void setImageArea(Rectangle imageArea) {
        this.imageArea = imageArea;
    }

    /**
     * Sets the area where the ROI in main image currently is.
     * 
     * @param roiArea
     *            Position and size of the ROI of the main image adopted to the
     *            overview.
     * */
    public void setROIArea(Rectangle roiArea) {
        this.roiArea = roiArea;
        if (!insideROIDragged) {
            setCursorDependingOnRegion(lastMouseLocation);
        }
    }

    public void setView(View newView) {
        super.setView(newView);
        SynchronizeView sview = ViewHelper.getViewAdapter(newView, SynchronizeView.class);
        if (sview != null) {
            observedView = sview.getObservedView();
        } else {
            observedView = null;
        }
    }

    /**
     * Checks if mouse cursor is placed over area where image data is available.
     * 
     * @param point
     *            Coordinates of the mouse cursor.
     * @return true if mouse cursor is located over this area.
     * */
    private boolean isCursorInImageArea(Point point) {
        return point.x >= imageArea.x && point.x <= imageArea.x + imageArea.width && point.y >= imageArea.y && point.y <= imageArea.y + imageArea.height;
    }

    /**
     * Checks if mouse cursor is placed over ROI.
     * 
     * @param point
     *            Coordinates of the mouse cursor.
     * @return true if mouse cursor is located over this area.
     * */
    private boolean isCursorInROIArea(Point point) {
        return point.x >= roiArea.x && point.x <= roiArea.x + roiArea.width && point.y >= roiArea.y && point.y <= roiArea.y + roiArea.height;
    }

    /**
     * Sets the image of the mouse cursor depending on its position.
     * 
     * @param point
     *            Coordinates of the mouse cursor.
     * */
    private void setCursorDependingOnRegion(Point point) {
        // check if mouse is inside image area
        if (isCursorInImageArea(point)) {

            // check if mouse is inside image area
            if (isCursorInROIArea(point)) {

                // Cursor is located in ROI area
                imagePanel.setCursor(openHandCursor);
                insideROI = true;
            } else {
                // Cursor is located in area where image data is available but
                // outside of ROI area
                imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                insideROI = false;
            }
        } else {
            // Cursor is located in area where no image data is available
            imagePanel.setCursor(Cursor.getDefaultCursor());
            insideROI = false;
        }
    }

    /**
     * Sets the region in main image depending on the mouse position in overview
     * image.
     * 
     * @param coordinate
     *            Coordinate of cursor in overview image.
     * */
    private void updateRegion(Point coordinate) {

        if (view == null || regionView == null || metaDataView == null) {
            return;
        }

        MetaData metaData = metaDataView.getMetaData();
        Region wholeRegion = regionView.getRegion();
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
        if (wholeRegion == null || viewportImageSize == null || metaData == null) {
            return;
        }

        Vector2dDouble newCenterRegion = new Vector2dDouble(wholeRegion.getCornerX() + (double) coordinate.x / (double) viewportImageSize.getWidth() * wholeRegion.getWidth(), wholeRegion.getCornerY() + (double) (viewportImageSize.getHeight() - coordinate.y) / (double) viewportImageSize.getHeight() * wholeRegion.getHeight());

        newCenterRegion = newCenterRegion.crop(metaData.getPhysicalLowerLeft(), metaData.getPhysicalUpperRight());

        SynchronizeView synchronizeView = view.getAdapter(SynchronizeView.class);

        if (synchronizeView == null || synchronizeView.getObservedView() == null) {
            return;
        }

        // get needed information and check values
        RegionView mainRegionView = synchronizeView.getObservedView().getAdapter(RegionView.class);
        if (mainRegionView == null) {
            return;
        }
        Region regionMainImage = mainRegionView.getRegion();
        if (regionMainImage == null) {
            return;
        }
        mainRegionView.setRegion(StaticRegion.createAdaptedRegion(newCenterRegion.subtract(regionMainImage.getSize().scale(0.5)), regionMainImage.getSize()), new ChangeEvent());

    }

    /**
     * Updates the view of the zoom controller.
     * 
     * It is always set to the observed view of the synchronize view
     */
    private void updateZoomController() {
        SynchronizeView synchronizeView = view.getAdapter(SynchronizeView.class);

        if (synchronizeView == null) {
            return;
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Mouse events
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            updateZoomController();
            if (interactionEnabled) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    zoomController.zoomSteps(observedView, 2);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    zoomController.zoomSteps(observedView, -2);
                }
            }
        }
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
        if (insideROI) {
            // update cursor
            imagePanel.setCursor(closedHandCursor);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
        // update region
        if (this.interactionEnabled) {
            updateRegion(e.getPoint());
        }

        // set flag that ROI rectangle will not be dragged anymore
        insideROIDragged = false;

        // update cursor
        setCursorDependingOnRegion(e.getPoint());
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent e) {
        lastMouseLocation = e.getPoint();

        if (insideROI) {
            // set flag for dragging the ROI rectangle
            insideROIDragged = true;

            // update region
            if (interactionEnabled) {
                updateRegion(e.getPoint());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(MouseEvent e) {
        lastMouseLocation = e.getPoint();
        // update cursor
        setCursorDependingOnRegion(e.getPoint());
    }

    /**
     * {@inheritDoc}
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (zoomController != null) {
            updateZoomController();
            if (this.interactionEnabled) {
                zoomController.zoomSteps(observedView, (int) (-Math.max(1, e.getUnitsToScroll() / 3) * Math.signum(e.getUnitsToScroll())));
            }
        }
    }
}
