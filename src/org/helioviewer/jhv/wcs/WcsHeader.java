package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

public final class WcsHeader {

    public enum Projection {
        TAN, ARC, AZP, ZPN, CAR, CEA;

        public boolean isSurfaceMap() {
            return this == CAR || this == CEA;
        }

        public boolean usesPv2() {
            return this == AZP || this == ZPN || this == CEA;
        }

        public static Projection fromCtype(String ctype1, String ctype2) {
            if (ctype1.endsWith("ARC") && ctype2.endsWith("ARC"))
                return ARC;
            if (ctype1.endsWith("AZP") && ctype2.endsWith("AZP"))
                return AZP;
            if (ctype1.endsWith("ZPN") && ctype2.endsWith("ZPN"))
                return ZPN;
            if (ctype1.endsWith("CAR") && ctype2.endsWith("CAR"))
                return CAR;
            if (ctype1.endsWith("CEA") && ctype2.endsWith("CEA"))
                return CEA;
            return TAN;
        }
    }

    public final Projection projection;
    public final float[] pv2;

    public final double zpnUpperEta;
    public final double zpnRadialLo;
    public final double zpnRadialHi;

    final double azpMu;
    final double azpSinGamma;
    final double azpCosGamma;
    final double azpMuPlus1;

    public final double unitsPerRad;
    final double radPerUnit;

    public final Vec2 crval;
    public final Quat crota;

    final double phi0;
    final double theta0;
    final double sinTheta0;
    final double cosTheta0;

    public WcsHeader(Projection _projection, float[] _pv2, double _unitsPerRad, Vec2 _crval, Quat _crota) {
        projection = _projection;
        pv2 = _pv2;

        boolean zpn = projection == Projection.ZPN;
        if (zpn) {
            zpnUpperEta = WcsProjection.zpnPrimaryBranchUpperEta(pv2);
            zpnRadialLo = pv2[0];
            zpnRadialHi = WcsProjection.zpnRadial(pv2, zpnUpperEta);
        } else {
            zpnUpperEta = 0;
            zpnRadialLo = 0;
            zpnRadialHi = 0;
        }

        boolean azp = projection == Projection.AZP;
        if (azp) {
            azpMu = pv2[1];
            double azpGamma = Math.toRadians(pv2[2]);
            azpSinGamma = Math.sin(azpGamma);
            azpCosGamma = Math.cos(azpGamma);
            azpMuPlus1 = azpMu + 1;
        } else {
            azpMu = 0;
            azpSinGamma = 0;
            azpCosGamma = 0;
            azpMuPlus1 = 0;
        }

        unitsPerRad = _unitsPerRad;
        radPerUnit = 1 / unitsPerRad;

        crval = _crval;
        crota = _crota;

        phi0 = crval.x * radPerUnit;
        theta0 = crval.y * radPerUnit;
        sinTheta0 = Math.sin(theta0);
        cosTheta0 = Math.cos(theta0);
    }

}
