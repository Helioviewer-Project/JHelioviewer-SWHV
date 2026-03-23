package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

final class WcsProjection {

    private WcsProjection() {
    }

    static Vec2 planeToHelioprojective(Context context, double x, double y) {
        Vec3 plane = context.crota.rotateVector(new Vec3(x, y, 0));
        return WcsInverse.inverseWcsPlaneToHelioprojective(context, plane.x, plane.y);
    }

    static final class Context {
        final Quat crota;
        final MetaData.WCSProjection projection;
        final float[] pv2;
        final double unitsPerRad;
        final double phi0;
        final double theta0;

        Context(MetaData metaData) {
            crota = metaData.getCROTA();
            projection = metaData.getWCSProjection();
            pv2 = metaData.getPV2();
            unitsPerRad = metaData.getWCSPlaneUnitsPerRad();
            Vec2 crval = metaData.getCRVAL();
            phi0 = crval.x / unitsPerRad;
            theta0 = crval.y / unitsPerRad;
        }
    }
}
