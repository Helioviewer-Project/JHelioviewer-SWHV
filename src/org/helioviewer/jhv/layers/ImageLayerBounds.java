package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ImageProjectionBounds;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.wcs.ImageBounds;

public final class ImageLayerBounds {

    private ImageLayerBounds() {}

    public static double getLargestPhysicalHeight() {
        if (Display.mode != ProjectionMode.Orthographic)
            return 1;

        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            size = Math.max(size, layer.getMetaData().getPhysicalRegion().height);
        }
        return size;
    }

    public static double getLargestRadialSize() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            size = Math.max(size, ImageBounds.radial(layer.getMetaData()));
        }
        return size;
    }

    public static Region getCenteredHpcScaleBounds() {
        Region bounds = getLargestHpcBounds();
        double halfWidth = Math.max(Math.abs(bounds.llx), Math.abs(bounds.urx));
        double halfHeight = Math.max(Math.abs(bounds.lly), Math.abs(bounds.ury));
        if (halfWidth <= 0)
            halfWidth = 5;
        if (halfHeight <= 0)
            halfHeight = 5;
        return new Region(-halfWidth, -halfHeight, 2 * halfWidth, 2 * halfHeight);
    }

    private static Region getLargestHpcBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            Region bounds = ImageProjectionBounds.hpc(layer.getMetaData());
            minX = Math.min(minX, bounds.llx);
            maxX = Math.max(maxX, bounds.urx);
            minY = Math.min(minY, bounds.lly);
            maxY = Math.max(maxY, bounds.ury);
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minY) || !Double.isFinite(maxY))
            return new Region(-5, -5, 10, 10);
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

}
