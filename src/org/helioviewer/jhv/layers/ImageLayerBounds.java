package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayMapBounds;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;
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
            size = Math.max(size, radial(layer.getMetaData()));
        }
        return size;
    }

    public static double getOneToOneCameraWidth(ImageLayer layer) {
        ImageData imageData = layer.getImageData();
        if (imageData == null)
            return 0;

        Viewport vp = Display.getActiveViewport();
        MetaData metaData = imageData.getMetaData();
        double physicalHeight = metaData.getPhysicalRegion().height;
        Quat mapRotation = Display.gridType.mapRotation(imageData.getViewpoint());
        double imageHeight = DisplayMapBounds.oneToOneHeight(Display.mode, mapRotation, metaData);
        double cameraWidth = vp.height * metaData.getUnitPerPixelY() * imageHeight / physicalHeight;
        if (Display.mode == ProjectionMode.Orthographic)
            return cameraWidth;

        double visibleHeight = visibleMapHeight(vp);
        return visibleHeight > 0 ? cameraWidth / visibleHeight : 0;
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

    private static double visibleMapHeight(Viewport vp) {
        if (Display.mode == ProjectionMode.Orthographic)
            return 1;
        if (Display.mode == ProjectionMode.HPC) {
            Region bounds = getCenteredHpcScaleBounds();
            double halfWidth = 0.5 * bounds.width;
            double halfHeight = 0.5 * bounds.height;
            halfHeight = Math.max(halfHeight, halfWidth / vp.aspect);
            return 2 * halfHeight;
        }

        ProjectionScale scale = Display.mode.scale;
        return Math.abs(scale.getInterpolatedYValue(1) - scale.getInterpolatedYValue(0));
    }

    private static Region getLargestHpcBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            Region bounds = ImageBounds.hpc(layer.getMetaData());
            minX = Math.min(minX, bounds.llx);
            maxX = Math.max(maxX, bounds.urx);
            minY = Math.min(minY, bounds.lly);
            maxY = Math.max(maxY, bounds.ury);
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minY) || !Double.isFinite(maxY))
            return new Region(-5, -5, 10, 10);
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    private static double radial(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        Vec2 crval = metaData.getWcsHeader().crval;
        double x0 = region.llx - crval.x;
        double x1 = region.urx - crval.x;
        double y0 = region.lly - crval.y;
        double y1 = region.ury - crval.y;
        return Math.max(
                Math.max(Math.hypot(x0, y0), Math.hypot(x1, y0)),
                Math.max(Math.hypot(x0, y1), Math.hypot(x1, y1)));
    }

}
