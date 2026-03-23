package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.math.PolarBasis;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

public final class DisplayMapBounds {

    private static final int EDGE_SAMPLES = 32;

    private DisplayMapBounds() {
    }

    public static double oneToOneHeight(ProjectionMode mode, GridType gridType, Position cameraViewpoint, MetaData metaData) {
        if (mode.isOrthographic() || mode.isPolar() || mode.isLogPolar())
            return metaData.getPhysicalRegion().height;
        return bounds(mode, gridType, cameraViewpoint, metaData).height;
    }

    private static Region bounds(ProjectionMode mode, GridType gridType, Position cameraViewpoint, MetaData metaData) {
        if (mode.isOrthographic())
            return metaData.getPhysicalRegion();
        if (mode.isHpc())
            return ImageBounds.hpc(metaData);

        WcsProjection.Context context = new WcsProjection.Context(metaData);
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
            updateBounds(bounds, context, metaViewpoint, mode, gridType, cameraViewpoint, x, y0);
            updateBounds(bounds, context, metaViewpoint, mode, gridType, cameraViewpoint, x, y1);
            updateBounds(bounds, context, metaViewpoint, mode, gridType, cameraViewpoint, x0, y);
            updateBounds(bounds, context, metaViewpoint, mode, gridType, cameraViewpoint, x1, y);
        }

        if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1]) || !Double.isFinite(bounds[2]) || !Double.isFinite(bounds[3])) {
            if (mode.isLatitudinal())
                return new Region(-180, -90, 360, 180);
            if (mode.isPolar() || mode.isLogPolar())
                return new Region(0, 0, 360, 1);
            return Region.DEFAULT;
        }
        return regionFromBounds(bounds);
    }

    private static void updateBounds(double[] bounds, WcsProjection.Context context, Position metaViewpoint, ProjectionMode mode, GridType gridType, Position cameraViewpoint, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(context, x, y);
        Vec3 world = helioprojectiveToWorld(metaViewpoint, helioprojective);
        if (world == null)
            return;

        Vec3 rotated = mapRotation(gridType, cameraViewpoint).rotateVector(world);
        double mapX;
        double mapY;
        if (mode.isLatitudinal()) {
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

    private static Quat mapRotation(GridType gridType, Position viewpoint) {
        return Quat.createXY(gridType == GridType.Viewpoint ? viewpoint.lat : 0, gridType.toLongitude(viewpoint));
    }

    private static Vec3 helioprojectiveToWorld(Position viewpoint, Vec2 helioprojective) {
        Vec3 ray = helioprojectiveToObserverRay(helioprojective);
        double b = viewpoint.distance * ray.z;
        double c = viewpoint.distance * viewpoint.distance - 1;
        double discriminant = b * b - c;
        if (discriminant < 0)
            return null;

        double root = Math.sqrt(discriminant);
        double t = -b - root;
        if (t <= 0)
            t = -b + root;
        if (t <= 0)
            return null;

        Vec3 view = new Vec3(t * ray.x, t * ray.y, viewpoint.distance + t * ray.z);
        return viewpoint.toQuat().rotateInverseVector(view);
    }

    private static Vec3 helioprojectiveToObserverRay(Vec2 helioprojective) {
        double phi = helioprojective.x;
        double theta = helioprojective.y;
        Vec3 ray = new Vec3(Math.tan(phi), Math.tan(theta) / Math.cos(phi), -1);
        ray.normalize();
        return ray;
    }
}
