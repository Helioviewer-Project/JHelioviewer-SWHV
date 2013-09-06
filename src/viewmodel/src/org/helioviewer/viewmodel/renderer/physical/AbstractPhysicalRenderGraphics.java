package org.helioviewer.viewmodel.renderer.physical;

import java.awt.image.BufferedImage;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Abstract base class for PhysicalRenderGraphics implementations.
 * 
 * <p>
 * This class implements some of the methods provided by PhysicalRenderGraphics
 * by simply mapping them to other methods. That way, every specific
 * implementation only has to implement the essential methods.
 * 
 * <p>
 * Also provides some internal functions for coordinate transformations.
 * 
 * @author Markus Langenberg
 * 
 */
public abstract class AbstractPhysicalRenderGraphics implements PhysicalRenderGraphics {

    private RegionView regionView;
    protected ViewportView viewportView;

    /**
     * Default constructor
     * 
     * @param view
     *            View to access information about the physical coordinate
     *            system.
     */
    public AbstractPhysicalRenderGraphics(View view) {
        regionView = view.getAdapter(RegionView.class);
        viewportView = view.getAdapter(ViewportView.class);
    }

    /**
     * {@inheritDoc}
     */
    public void drawLine(Vector2dDouble p0, Vector2dDouble p1) {
        drawLine(p0.getX(), p0.getY(), p1.getX(), p1.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawRectangle(Vector2dDouble position, Vector2dDouble size) {
        drawRectangle(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillRectangle(Vector2dDouble position, Vector2dDouble size) {
        fillRectangle(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawOval(Vector2dDouble position, Vector2dDouble size) {
        drawOval(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(Vector2dDouble position, Vector2dDouble size) {
        fillOval(position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dDouble position) {
        drawImage(image, position.getX(), position.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dDouble position, float scale) {
        drawImage(image, position.getX(), position.getY(), scale);
    }

    /**
     * {@inheritDoc}
     */
    public void drawImage(BufferedImage image, Vector2dDouble position, Vector2dDouble size) {
        drawImage(image, position.getX(), position.getY(), size.getX(), size.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void drawText(String text, Vector2dDouble position) {
        drawText(text, position.getX(), position.getY());
    }

    /**
     * Converts from physical coordinates to screen coordinates.
     * 
     * @param x
     *            x-coordinate of point in physical coordinates.
     * @param y
     *            y-coordinate of point in physical coordinates.
     * @return point in screen coordinates
     */
    protected Vector2dInt convertPhysicalToScreen(double x, double y) {
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion());

        Vector2dInt offset = ViewHelper.convertImageToScreenDisplacement(-regionView.getRegion().getUpperLeftCorner().getX(), regionView.getRegion().getUpperLeftCorner().getY(), regionView.getRegion(), viewportImageSize);

        return ViewHelper.convertImageToScreenDisplacement(x, y, regionView.getRegion(), viewportImageSize).add(offset);
    }

    /**
     * Converts from physical coordinates to screen coordinates.
     * 
     * @param vector
     *            point in physical coordinates.
     * @return point in screen coordinates
     */
    protected Vector2dInt convertPhysicalToScreen(Vector2dDouble vector) {
        return ViewHelper.convertImageToScreenDisplacement(vector, regionView.getRegion(), ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion()));
    }

    /**
     * Converts from screen coordinates to physical coordinates.
     * 
     * @param x
     *            x-coordinate of point in screen coordinates.
     * @param y
     *            y-coordinate of point in screen coordinates.
     * @return point in physical coordinates
     */
    protected Vector2dDouble convertScreenToPhysical(int x, int y) {
        return ViewHelper.convertScreenToImageDisplacement(x, y, regionView.getRegion(), ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion()));
    }

    /**
     * Converts from screen coordinates to physical coordinates.
     * 
     * @param vector
     *            point in screen coordinates.
     * @return point in physical coordinates
     */
    protected Vector2dDouble convertScreenToPhysical(Vector2dInt vector) {
        return ViewHelper.convertScreenToImageDisplacement(vector, regionView.getRegion(), ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion()));
    }
}
