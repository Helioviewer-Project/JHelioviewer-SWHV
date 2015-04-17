package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.math.GL3DVec2d;
import org.helioviewer.viewmodel.region.Region;

/**
 * Implementation of MetaData representing images without information about
 * their physical size.
 *
 * <p>
 * The purpose of this implementation is to represent images without physical
 * informations. It can be seen as a fallback solution. There are no
 * informations available which are not provided by the image data object
 * itself, but this implementation is provided to stay consistent.
 *
 * @author Ldwig Schmidt
 *
 */
public class PixelBasedMetaData extends AbstractMetaData implements ImageSizeMetaData {
    private double unitsPerPixel = 1.0;
    private final Vector2dInt resolution;

    /**
     * Constructor, setting the size. The position is set to (0,0) by default.
     *
     * @param newWidth
     *            Width of the corresponding image
     * @param newHeight
     *            Height of the corresponding image
     */
    public PixelBasedMetaData(int newWidth, int newHeight) {
        super(0, 0, newWidth, newHeight);
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCorner
     *            Lower left corner of the corresponding image
     * @param newSizeVector
     *            Size of the corresponding image
     */
    public PixelBasedMetaData(Vector2dInt newLowerLeftCorner, Vector2dInt newSizeVector) {
        super(new GL3DVec2d(newLowerLeftCorner), new GL3DVec2d(newSizeVector));
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCornerX
     *            Lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Lower left y-coordinate of the corresponding image
     * @param newWidth
     *            Width of the corresponding image
     * @param newHeight
     *            Height of the corresponding image
     */
    public PixelBasedMetaData(int newLowerLeftCornerX, int newLowerLeftCornerY, int newWidth, int newHeight) {
        super(newLowerLeftCornerX, newLowerLeftCornerY, newWidth, newHeight);
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCorner
     *            Lower left corner of the corresponding image
     * @param newWidth
     *            Width of the corresponding image
     * @param newHeight
     *            Height of the corresponding image
     */
    public PixelBasedMetaData(Vector2dInt newLowerLeftCorner, int newWidth, int newHeight) {
        super(new GL3DVec2d(newLowerLeftCorner), newWidth, newHeight);
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCornerX
     *            Lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Lower left y-coordinate of the corresponding image
     * @param newSizeVector
     *            Size of the corresponding image
     */
    public PixelBasedMetaData(int newLowerLeftCornerX, int newLowerLeftCornerY, Vector2dInt newSizeVector) {
        super(newLowerLeftCornerX, newLowerLeftCornerY, new GL3DVec2d(newSizeVector));
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newRectangle
     *            Full rectangle of the corresponding image
     */
    public PixelBasedMetaData(RectangleDouble newRectangle) {
        super(newRectangle);
        resolution = new Vector2dInt(getPhysicalImageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2dInt getResolution() {
        return resolution;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getUnitsPerPixel() {
        return unitsPerPixel;
    }

    /**
     * Recalculates the virtual physical region of this pixel based image so it
     * just covers the given region completely
     *
     * @param region
     *            The region which this image should cover
     */
    public void updatePhysicalRegion(Region region) {
        double unitsPerPixelX = region.getWidth() / getResolution().getX();
        double unitsPerPixelY = region.getHeight() / getResolution().getY();
        double newUnitsPerPixel = Math.max(unitsPerPixelX, unitsPerPixelY);
        setPhysicalImageSize(GL3DVec2d.scale(getPhysicalImageSize(), newUnitsPerPixel / unitsPerPixel));
        setPhysicalLowerLeftCorner(new GL3DVec2d(region.getCornerX(), region.getCornerY() - getPhysicalImageHeight() + region.getHeight()));
        this.unitsPerPixel = newUnitsPerPixel;
    }

}
