package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

public final class WcsHeader {

    public enum Projection {
        TAN,
        AZP,
        ZPN,
        CAR;

        public boolean usesPv2() {
            return this == AZP || this == ZPN;
        }

        public static Projection fromCtypePair(String ctype1, String ctype2) {
            if (ctype1.endsWith("CAR") && ctype2.endsWith("CAR"))
                return CAR;
            if (ctype1.endsWith("AZP") && ctype2.endsWith("AZP"))
                return AZP;
            if (ctype1.endsWith("ZPN") && ctype2.endsWith("ZPN"))
                return ZPN;
            return TAN;
        }
    }

    public final Projection projection;
    public final float[] pv2;
    public final double unitsPerRad;
    public final Vec2 crval;
    public final Quat crota;
    final double phi0;
    final double theta0;

    public WcsHeader(Projection _projection, float[] _pv2, double _unitsPerRad, Vec2 _crval, Quat _crota) {
        projection = _projection;
        pv2 = _pv2;
        unitsPerRad = _unitsPerRad;
        crval = _crval;
        crota = _crota;
        phi0 = crval.x / unitsPerRad;
        theta0 = crval.y / unitsPerRad;
    }

}
