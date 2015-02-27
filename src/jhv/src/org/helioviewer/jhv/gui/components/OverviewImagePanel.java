package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.controller.OverviewImagePanelMousePanController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.SynchronizedROIChangedReason;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.bufferedimage.JavaImagePanel;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * This class represents an image component that is used to display the overview
 * of an image.
 *
 * @author Stephan Pagel
 *
 */
public class OverviewImagePanel extends BasicImagePanel {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    // default serialVersionUID
    private static final long serialVersionUID = 1L;

    private boolean drawRect = true;

    // buffered data
    private ViewportImageSize viewportImageSize;
    private Vector2dInt offset;

    private final OverviewImagePanelPostRenderer postRenderer = new OverviewImagePanelPostRenderer();
    private final OverviewImagePanelMousePanController inputControllerPanning = new OverviewImagePanelMousePanController();

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public OverviewImagePanel() {

        // call constructor of super class
        super();

        renderedImageComponent = new JavaImagePanel();
        add(renderedImageComponent);

        // add post renderer
        addPostRenderer(postRenderer);

        // add a mouse input controller to the panel
        setInputController(inputControllerPanning);
    }

    public void enableInteraction() {
        this.inputControllerPanning.setInteractionEnabled(true);
    }

    public void disableInteraction() {
        this.inputControllerPanning.setInteractionEnabled(false);
    }

    /**
     * {@inheritDoc}
     *
     * This method is overridden to protect the internal input controller. Input
     * controller for this class are handled by this class by its own.
     */

    @Override
    public void setInputController(ImagePanelInputController newInputController) {

        if (newInputController == inputControllerPanning)
            super.setInputController(newInputController);
    }

    /**
     * {@inheritDoc}
     *
     * Computes the ROI rectangle for the overview image.
     * */

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        // Show rendered image centered
        if (renderedImageComponent != null) {

            boolean layerChanged = aEvent.reasonOccurred(LayerChangedReason.class);

            if (layerChanged) {
                drawRect = sender.getAdapter(LayeredView.class).getNumLayers() > 0 && sender.getAdapter(LayeredView.class).getNumberOfVisibleLayer() > 0;
            }

            if (drawRect) {

                // get size of view port of overview image
                ViewportImageSize tempViewportImageSize = ViewHelper.calculateViewportImageSize(componentView);

                if (tempViewportImageSize != null) {
                    viewportImageSize = tempViewportImageSize;

                    // compute offset, so image will be displayed centered
                    offset = computeOffset(viewportImageSize);

                    componentView.setOffset(offset);

                    inputControllerPanning.setImageArea(new Rectangle(offset.getX(), offset.getY(), viewportImageSize.getWidth(), viewportImageSize.getHeight()));

                }

                SynchronizedROIChangedReason reason = aEvent.getLastChangedReasonByType(SynchronizedROIChangedReason.class);

                // compute rectangle of current ROI
                if (regionView.getRegion() != null && reason != null && reason.getRegionToSynchronize() != null) {
                    //Catch occasional faulty rectangle.
                    try {
                        Rectangle roi = calculateRectangle(viewportImageSize, reason.getRegionToSynchronize());
                        roi.x = Math.abs(roi.x) + offset.getX();
                        roi.y = Math.abs(roi.y) + offset.getY();

                        roi.width--;
                        roi.height--;
                        roi.width = roi.width < 1 ? 1 : roi.width;
                        roi.height = roi.height < 1 ? 1 : roi.height;

                        Vector2dInt center = calculateCenter(viewportImageSize, reason.getRegionToSynchronize());
                        postRenderer.setROIRectangle(roi);
                        postRenderer.setCenterRectangle(center);
                        inputControllerPanning.setROIArea(roi);
                    } catch (NullPointerException e) {
                        Log.error(e);
                    }
                }
            } else {
                // define "dummy" rectangle
                Rectangle rect = new Rectangle(0, 0);

                // "remove" region of interest
                postRenderer.setROIRectangle(rect);
                postRenderer.setCenterRectangle(null);
                inputControllerPanning.setROIArea(rect);

                // "remove" image region
                inputControllerPanning.setImageArea(rect);
            }
        }
    }

    /**
     * Computes the offset that image will be displayed centered.
     *
     * @param viewportImageSize
     *            image size
     * @return offset that image will be displayed centered.
     */
    private Vector2dInt computeOffset(ViewportImageSize viewportImageSize) {
        int x = (getViewport().getWidth() - viewportImageSize.getWidth()) / 2;
        x = x >= 0 ? x : 0;

        int y = (getViewport().getHeight() - viewportImageSize.getHeight()) / 2;
        y = y >= 0 ? y : 0;

        return new Vector2dInt(x, y);
    }

    /**
     * Computes the size and position of the rectangle which indicates the
     * current ROI.
     *
     * @param viewportImageSize
     *            Size of the view port of the overview image
     * @param regionMainImage
     *            The region which is currently shown in the main image
     * @return position and size of the rectangle which indicates the current
     *         ROI.
     * */
    private Rectangle calculateRectangle(ViewportImageSize viewportImageSize, Region regionMainImage) {

        Rectangle rect = new Rectangle();
        regionMainImage = ViewHelper.cropInnerRegionToOuterRegion(regionMainImage, regionView.getRegion());

        rect.x = (int) Math.round(((regionView.getRegion().getCornerX() - regionMainImage.getUpperLeftCorner().getX()) / regionView.getRegion().getWidth()) * viewportImageSize.getWidth());
        rect.width = (int) Math.round((regionMainImage.getWidth() / regionView.getRegion().getWidth()) * viewportImageSize.getWidth());
        rect.y = (int) Math.round(((regionView.getRegion().getUpperLeftCorner().getY() - regionMainImage.getUpperLeftCorner().getY()) / regionView.getRegion().getHeight()) * viewportImageSize.getHeight());
        rect.height = (int) Math.round((regionMainImage.getHeight() / regionView.getRegion().getHeight()) * viewportImageSize.getHeight());
        return rect;
    }

    /**
     * Method will be called when component was resized. Resets the viewport and
     * region.
     */

    @Override
    public void componentResized(ComponentEvent e) {

        if (viewportView != null) {
            if (updateViewportView)
                viewportView.setViewport(getViewport(), new ChangeEvent());
        }
        if (metaDataView != null && regionView != null && getViewport() != null) {
            MetaData metaData = metaDataView.getMetaData();
            if (metaData != null) {
                Region r = ViewHelper.expandRegionToViewportAspectRatio(viewportView.getViewport(), metaData.getPhysicalRegion(), metaData);
                regionView.setRegion(r, new ChangeEvent());
            }
        }
        repaint();
    }

    /**
     * Returns the provided viewport of this component
     *
     * @return provided viewport of this component.
     * */
    @Override
    public Viewport getViewport() {
        return StaticViewport.createAdaptedViewport(Math.max(1, getWidth() - 2), Math.max(1, getHeight() - 2));
    }

    /**
     * Computes the the center of the main image region translated to overview
     * panel coordinates.
     *
     * @param viewportImageSize
     *            Size of the view port of the overview image
     * @param regionMainImage
     *            The region which is currently shown in the main image
     * @return center of the main image region translated to overview panel
     *         coordinates
     * */
    private Vector2dInt calculateCenter(ViewportImageSize viewportImageSize, Region regionMainImage) {

        Vector2dDouble centerMainImage = regionMainImage.getLowerLeftCorner().add(regionMainImage.getSize().scale(0.5));

        int y = (int) ((regionView.getRegion().getUpperLeftCorner().getY() - centerMainImage.getY()) / regionView.getRegion().getHeight() * viewportImageSize.getHeight());
        int x = (int) ((centerMainImage.getX() - regionView.getRegion().getUpperLeftCorner().getX()) / regionView.getRegion().getWidth() * viewportImageSize.getWidth());

        return new Vector2dInt(x, y);
    }

    /**
     * Class represents the post renderer for the overview image panel. Contains
     * the drawing management for the ROI rectangle.
     *
     * @author Stephan Pagel
     * */
    private class OverviewImagePanelPostRenderer implements ScreenRenderer {

        // ///////////////////////////////////////////////////////////////////////
        // Definitions
        // ///////////////////////////////////////////////////////////////////////

        private Rectangle rect;
        private Vector2dInt center;
        private final Color color = Color.YELLOW;
        private final Vector2dInt centerOvalRadius = new Vector2dInt(3, 3);
        private final Vector2dInt centerOvalDiameter = centerOvalRadius.scale(2).add(new Vector2dInt(1, 1));

        // ///////////////////////////////////////////////////////////////////////
        // Methods
        // ///////////////////////////////////////////////////////////////////////

        /**
         * Sets the last position and size of the rectangle.
         *
         * @param rect
         *            size and position of the rectangle.
         */
        public void setROIRectangle(Rectangle rect) {
            this.rect = rect;
        }

        /**
         * Sets the center of the main image region in overview panel
         * coordinates
         *
         * @param center
         *            center of the main image region in overview panel
         *            coordinates
         */

        public void setCenterRectangle(Vector2dInt center) {
            this.center = center;
        }

        /**
         * Draws the rectangle.
         *
         * @param g
         *            Graphics object where to draw the rectangle.
         */
        @Override
        public void render(ScreenRenderGraphics g) {

            if (rect != null) {
                g.setColor(color);
                g.drawRectangle(rect.x, rect.y, rect.width, rect.height);
            }
            if (center != null) {
                g.setColor(color);
                g.drawOval(center.subtract(centerOvalRadius), centerOvalDiameter);
            }
        }
    }
}
