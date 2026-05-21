package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.wcs.ImageBounds;
import org.helioviewer.jhv.wcs.WcsHeader;
import org.helioviewer.jhv.wcs.WcsProjection;

public final class DisplayMapBounds {

    private static final int EDGE_SAMPLES = 32;

    private DisplayMapBounds() {}

    public static double oneToOneHeight(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
        if (mode == ProjectionMode.Orthographic)
            return metaData.getPhysicalRegion().height;
        return bounds(mode, mapRotation, metaData).height;
    }

    private static Region bounds(ProjectionMode mode, Quat mapRotation, MetaData metaData) {
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
        Vec3 world = NonOrthoProjection.helioprojectiveToWorld(metaViewpoint, helioprojective.x, helioprojective.y);
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
}
