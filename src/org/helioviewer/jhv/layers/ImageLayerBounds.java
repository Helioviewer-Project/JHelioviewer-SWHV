package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.wcs.ImageBounds;
import org.helioviewer.jhv.wcs.WcsHeader;
import org.helioviewer.jhv.wcs.WcsProjection;

public final class ImageLayerBounds {

    private static final int EDGE_SAMPLES = 32;

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

    public static double getOneToOneCameraWidth(ImageLayer layer) {
        ImageData imageData = layer.getImageData();
        if (imageData == null)
            return 0;

        Viewport vp = Display.getActiveViewport();
        MetaData metaData = imageData.getMetaData();
        double physicalHeight = metaData.getPhysicalRegion().height;
        Quat mapRotation = Display.gridType.mapRotation(GLRenderer.getDisplayedViewpoint());
        double imageHeight = oneToOneHeight(Display.mode, mapRotation, metaData);
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

    private static double oneToOneHeight(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
        if (mode == ProjectionMode.Orthographic)
            return metaData.getPhysicalRegion().height;
        return projectedBounds(mode, mapRotation, metaData).height;
    }

    private static Region projectedBounds(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
        return switch (mode) {
            case Orthographic -> metaData.getPhysicalRegion();
            case HPC -> ImageBounds.hpc(metaData);
            default -> calculateNonOrthoBounds(mode, mapRotation, metaData);
        };
    }

    private static Region calculateNonOrthoBounds(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
        WcsHeader wcsHeader = metaData.getWcsHeader();
        Region region = metaData.getPhysicalRegion();
        double x0 = region.llx;
        double x1 = region.urx;
        double y0 = region.lly;
        double y1 = region.ury;
        double[] bounds = {
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };

        Position metaViewpoint = metaData.getViewpoint();
        for (int i = 0; i <= EDGE_SAMPLES; i++) {
            double t = i / (double) EDGE_SAMPLES;
            double x = x0 + t * (x1 - x0);
            double y = y0 + t * (y1 - y0);
            updateBounds(bounds, wcsHeader, metaViewpoint, mode, mapRotation, x, y0);
            updateBounds(bounds, wcsHeader, metaViewpoint, mode, mapRotation, x, y1);
            updateBounds(bounds, wcsHeader, metaViewpoint, mode, mapRotation, x0, y);
            updateBounds(bounds, wcsHeader, metaViewpoint, mode, mapRotation, x1, y);
        }

        if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1]) || !Double.isFinite(bounds[2]) || !Double.isFinite(bounds[3])) {
            return switch (mode) {
                case Latitudinal -> new Region(-180, -90, 360, 180);
                case Polar, LogPolar -> new Region(0, 0, 360, 1);
                default -> Region.DEFAULT;
            };
        }
        return regionFromBounds(bounds);
    }

    private static void updateBounds(double[] bounds, WcsHeader wcsHeader, Position metaViewpoint, ProjectionMode mode, Quat mapRotation, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(wcsHeader, x, y);
        Vec3 world = WcsProjection.helioprojectiveToWorld(metaViewpoint, helioprojective.x, helioprojective.y);
        if (world == null)
            return;

        Vec3 rotated = mapRotation.rotateVector(world);
        double mapX;
        double mapY;
        if (mode == ProjectionMode.Latitudinal) {
            mapX = Math.toDegrees(SphericalCoords.longitude(rotated));
            mapY = Math.toDegrees(SphericalCoords.latitude(rotated));
        } else {
            mapX = Math.toDegrees(PolarBasis.angle(rotated));
            mapY = Math.sqrt(rotated.x * rotated.x + rotated.y * rotated.y);
        }

        bounds[0] = Math.min(bounds[0], mapX);
        bounds[1] = Math.max(bounds[1], mapX);
        bounds[2] = Math.min(bounds[2], mapY);
        bounds[3] = Math.max(bounds[3], mapY);
    }

    private static Region regionFromBounds(double[] bounds) {
        return new Region(bounds[0], bounds[2],
                Math.max(Math.nextUp(0.0), bounds[1] - bounds[0]),
                Math.max(Math.nextUp(0.0), bounds[3] - bounds[2]));
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

}
