package org.helioviewer.viewmodel.metadata;

import java.util.Date;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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
public class PixelBasedMetaData extends AbstractMetaData {

    private double unitsPerPixel = 1.0;
    private final GL3DQuatd localRotation = new GL3DQuatd();

    private final int pixelWidth;
    private final int pixelHeight;

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

        pixelWidth = newWidth;
        pixelHeight = newHeight;
        this.dateTime = new ImmutableDateTime(new Date().getTime());
    }

    public GL3DQuatd getLocalRotation() {
        return localRotation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getUnitsPerPixel() {
        return unitsPerPixel;
    }

    @Override
    public int getPixelHeight() {
        return pixelHeight;
    }

    @Override
    public int getPixelWidth() {
        return pixelWidth;
    }

    /**
     * Recalculates the virtual physical region of this pixel based image so it
     * just covers the given region completely
     *
     * @param region
     *            The region which this image should cover
     */
    public void updatePhysicalRegion(Region region) {
        double unitsPerPixelX = region.getWidth() / pixelWidth;
        double unitsPerPixelY = region.getHeight() / pixelHeight;
        double newUnitsPerPixel = Math.max(unitsPerPixelX, unitsPerPixelY);

        setPhysicalSize(GL3DVec2d.scale(new GL3DVec2d(getPhysicalWidth(), getPhysicalHeight()), newUnitsPerPixel / unitsPerPixel));
        setPhysicalLowerLeftCorner(new GL3DVec2d(region.getCornerX(), region.getCornerY() - getPhysicalHeight() + region.getHeight()));
        unitsPerPixel = newUnitsPerPixel;
    }

}
