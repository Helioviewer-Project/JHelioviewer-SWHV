package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.math.Vec2d;

public class PixelBasedMetaData extends AbstractMetaData {
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
    }

    /**
     * Recalculates the virtual physical region of this pixel based image so it
     * just covers the given region completely
     *
     * @param region
     *            The region which this image should cover
     */
    public void updatePhysicalRegion(Region region) {
        double unitPerPixelX = region.getWidth() / pixelWidth;
        double unitPerPixelY = region.getHeight() / pixelHeight;
        double newUnitPerPixel = Math.max(unitPerPixelX, unitPerPixelY);

        setPhysicalSize(Vec2d.scale(getPhysicalSize(), newUnitPerPixel / unitPerPixel));
        setPhysicalLowerLeftCorner(new Vec2d(region.getLowerLeftCorner().x, region.getLowerLeftCorner().y - getPhysicalSize().y + region.getHeight()));
        unitPerPixel = newUnitPerPixel;
    }

}
