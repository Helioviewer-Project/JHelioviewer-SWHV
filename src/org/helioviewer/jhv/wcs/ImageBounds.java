package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

public final class ImageBounds {

    private static final int EDGE_SAMPLES = 32;

    private ImageBounds() {}

    public static double radial(MetaData metaData) {
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

    public static Region latitudinal(MetaData metaData, Quat mapRotation) {
        return surfaceBounds(metaData, mapRotation, true);
    }

    public static Region polar(MetaData metaData, Quat mapRotation) {
        return surfaceBounds(metaData, mapRotation, false);
    }

    private static Region surfaceBounds(MetaData metaData, Quat mapRotation, boolean latitudinal) {
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
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, latitudinal, x, y0);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, latitudinal, x, y1);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, latitudinal, x0, y);
            updateSurfaceBounds(bounds, wcsHeader, viewpoint, mapRotation, latitudinal, x1, y);
        }

        if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1]) || !Double.isFinite(bounds[2]) || !Double.isFinite(bounds[3]))
            return latitudinal ? new Region(-180, -90, 360, 180) : new Region(0, 0, 360, 1);
        return regionFromBounds(bounds);
    }

    private static void updateSurfaceBounds(double[] bounds, WcsHeader wcsHeader, Position viewpoint, Quat mapRotation, boolean latitudinal, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(wcsHeader, x, y);
        Vec3 world = WcsProjection.helioprojectiveToWorld(viewpoint, helioprojective.x, helioprojective.y);
        if (world == null)
            return;

        Vec3 rotated = mapRotation.rotateVector(world);
        double mapX;
        double mapY;
        if (latitudinal) {
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
