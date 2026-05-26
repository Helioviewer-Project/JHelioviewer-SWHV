package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.wcs.WcsHeader;
import org.helioviewer.jhv.wcs.WcsProjection;

public final class ImageProjectionBounds {

    private static final int EDGE_SAMPLES = 32;

    private ImageProjectionBounds() {}

    public static double oneToOneCameraWidth(ImageLayer layer, Viewport vp, ProjectionMode mode, GridType gridType, Position viewpoint) {
        ImageData imageData = layer.getImageData();
        if (imageData == null)
            return 0;

        MetaData metaData = imageData.getMetaData();
        Region physicalRegion = metaData.getPhysicalRegion();
        Quat mapRotation = gridType.mapRotation(viewpoint);
        double imageHeight = projected(mode, mapRotation, metaData).height;
        double cameraWidth = vp.height * metaData.getUnitPerPixelY() * imageHeight / physicalRegion.height;
        if (mode == ProjectionMode.Orthographic)
            return cameraWidth;

        double viewportHeight = projectedViewportHeight(vp, mode);
        return viewportHeight > 0 ? cameraWidth / viewportHeight : 0;
    }

    public static Region projected(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
        return switch (mode) {
            case Orthographic -> metaData.getPhysicalRegion();
            case HPC -> hpc(metaData);
            case Latitudinal -> surfaceBounds(metaData, mapRotation, ProjectedMapProjection.Kind.LATITUDINAL);
            case Polar, LogPolar -> surfaceBounds(metaData, mapRotation, ProjectedMapProjection.Kind.POLAR);
        };
    }

    private static double projectedViewportHeight(Viewport vp, ProjectionMode mode) {
        if (mode == ProjectionMode.HPC) {
            Region bounds = ImageLayerBounds.getCenteredHpcScaleBounds();
            double halfWidth = 0.5 * bounds.width;
            double halfHeight = 0.5 * bounds.height;
            halfHeight = Math.max(halfHeight, halfWidth / vp.aspect);
            return 2 * halfHeight;
        }

        ProjectionScale scale = mode.scale;
        return Math.abs(scale.getInterpolatedYValue(1) - scale.getInterpolatedYValue(0));
    }

    public static Region hpc(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        WcsHeader wcsHeader = metaData.getWcsHeader();
        double x0 = region.llx;
        double x1 = region.urx;
        double y0 = region.lly;
        double y1 = region.ury;
        double xm = 0.5 * (x0 + x1);
        double ym = 0.5 * (y0 + y1);
        double[] bounds = {
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        updateHpcBounds(bounds, wcsHeader, x0, y0);
        updateHpcBounds(bounds, wcsHeader, x1, y0);
        updateHpcBounds(bounds, wcsHeader, x0, y1);
        updateHpcBounds(bounds, wcsHeader, x1, y1);
        updateHpcBounds(bounds, wcsHeader, xm, y0);
        updateHpcBounds(bounds, wcsHeader, xm, y1);
        updateHpcBounds(bounds, wcsHeader, x0, ym);
        updateHpcBounds(bounds, wcsHeader, x1, ym);
        return regionFromBounds(bounds);
    }

    private static void updateHpcBounds(double[] bounds, WcsHeader wcsHeader, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(wcsHeader, x, y);
        double hpcX = Math.toDegrees(helioprojective.x);
        double hpcY = Math.toDegrees(helioprojective.y);
        bounds[0] = Math.min(bounds[0], hpcX);
        bounds[1] = Math.max(bounds[1], hpcX);
        bounds[2] = Math.min(bounds[2], hpcY);
        bounds[3] = Math.max(bounds[3], hpcY);
    }

    private static Region surfaceBounds(MetaData metaData, Quat mapRotation, ProjectedMapProjection.Kind kind) {
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

        Position viewpoint = metaData.getViewpoint();
        for (int i = 0; i <= EDGE_SAMPLES; i++) {
            double t = i / (double) EDGE_SAMPLES;
            double x = x0 + t * (x1 - x0);
            double y = y0 + t * (y1 - y0);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, kind, x, y0);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, kind, x, y1);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, kind, x0, y);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, kind, x1, y);
        }

        if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1]) || !Double.isFinite(bounds[2]) || !Double.isFinite(bounds[3]))
            return switch (kind) {
                case LATITUDINAL -> new Region(-180, -90, 360, 180);
                case POLAR -> new Region(0, 0, 360, 1);
                case HPC -> throw new IllegalArgumentException();
            };
        return regionFromBounds(bounds);
    }

    private static void updateSurfaceBounds(double[] bounds, WcsHeader wcsHeader, Position viewpoint, Quat mapRotation, ProjectedMapProjection.Kind kind, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(wcsHeader, x, y);
        Vec3 world = WcsProjection.helioprojectiveToWorld(viewpoint, helioprojective.x, helioprojective.y);
        if (world == null)
            return;

        Vec3 rotated = mapRotation.rotateVector(world);
        Vec2 pt = switch (kind) {
            case LATITUDINAL -> ProjectedMapProjection.latitudinalPoint(rotated);
            case POLAR -> ProjectedMapProjection.polarPoint(rotated);
            case HPC -> throw new IllegalArgumentException();
        };

        bounds[0] = Math.min(bounds[0], pt.x);
        bounds[1] = Math.max(bounds[1], pt.x);
        bounds[2] = Math.min(bounds[2], pt.y);
        bounds[3] = Math.max(bounds[3], pt.y);
    }

    private static Region regionFromBounds(double[] bounds) {
        return new Region(bounds[0], bounds[2],
                Math.max(Math.nextUp(0.0), bounds[1] - bounds[0]),
                Math.max(Math.nextUp(0.0), bounds[3] - bounds[2]));
    }
}
