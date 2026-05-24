package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

public final class WcsProjection {

    private static final int ZPN_BISECTION_STEPS = 50;

    private WcsProjection() {}

    public static Vec2 planeToHelioprojective(WcsHeader wcsHeader, double x, double y) {
        Quat crota = wcsHeader.crota;
        double vx = crota.w * x - crota.z * y;
        double vy = crota.z * x + crota.w * y;
        double vz = crota.x * y - crota.y * x;
        double planeX = (vz * crota.y - vy * crota.z) * 2 + x;
        double planeY = (vx * crota.z - vz * crota.x) * 2 + y;
        return inverseWcsPlaneToHelioprojective(wcsHeader, planeX, planeY);
    }

    private static Vec2 inverseWcsPlaneToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        if (wcsHeader.projection == WcsHeader.Projection.ARC)
            return inverseArcToHelioprojective(wcsHeader, planeX, planeY);
        if (wcsHeader.projection == WcsHeader.Projection.AZP)
            return inverseAzpToHelioprojective(wcsHeader, planeX, planeY);
        if (wcsHeader.projection == WcsHeader.Projection.ZPN)
            return inverseZpnToHelioprojective(wcsHeader, planeX, planeY);
        return inverseTanToHelioprojective(wcsHeader, planeX, planeY);
    }

    private static Vec2 inverseTanToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX * wcsHeader.radPerUnit;
        double y = planeY * wcsHeader.radPerUnit;
        double rho2 = x * x + y * y;
        if (rho2 == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double cosNativeDistance = 1 / Math.sqrt(1 + rho2);
        double nativeX = x * cosNativeDistance;
        double nativeY = y * cosNativeDistance;
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 inverseArcToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX * wcsHeader.radPerUnit;
        double y = planeY * wcsHeader.radPerUnit;
        double radial = Math.sqrt(x * x + y * y);
        if (radial == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double nativeRadius = Math.sin(radial);
        double nativeX = nativeRadius * x / radial;
        double nativeY = nativeRadius * y / radial;
        double cosNativeDistance = Math.cos(radial);
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 inverseAzpToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX * wcsHeader.radPerUnit;
        double y = planeY * wcsHeader.radPerUnit;
        if (x * x + y * y == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double mu = wcsHeader.azpMu;
        double cosGamma = wcsHeader.azpCosGamma;
        double muPlus1 = wcsHeader.azpMuPlus1;
        double a = 1 + y * wcsHeader.azpSinGamma / muPlus1;
        double k = (x * x + y * y * cosGamma * cosGamma) / (muPlus1 * muPlus1 * a * a);
        double discriminant = Math.max(0, 1 + k * (1 - mu * mu));
        double cosNativeDistance = (-k * mu + Math.sqrt(discriminant)) / (1 + k);
        double denom = (mu + cosNativeDistance) / a;
        double nativeX = x * denom / muPlus1;
        double nativeY = y * cosGamma * denom / muPlus1;
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 nativeToHelioprojective(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        return new Vec2(
                wcsHeader.phi0 + Math.atan2(nativeX, cosNativeDistance * wcsHeader.cosTheta0 - nativeY * wcsHeader.sinTheta0),
                Math.asin(cosNativeDistance * wcsHeader.sinTheta0 + nativeY * wcsHeader.cosTheta0));
    }

    private static Vec2 inverseZpnToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX * wcsHeader.radPerUnit;
        double y = planeY * wcsHeader.radPerUnit;
        double radial = Math.sqrt(x * x + y * y);
        double nativeDistance = inverseZpnPrimaryBranch(wcsHeader, radial);
        if (nativeDistance == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double nativeRadius = Math.sin(nativeDistance);
        double nativeX = nativeRadius * x / radial;
        double nativeY = nativeRadius * y / radial;
        double cosNativeDistance = Math.cos(nativeDistance);
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static double inverseZpnPrimaryBranch(WcsHeader wcsHeader, double radial) {
        double lo = 0;
        double hi = wcsHeader.zpnUpperEta;
        double target = clampZpnRadial(radial, wcsHeader.zpnRadialLo, wcsHeader.zpnRadialHi);
        for (int i = 0; i < ZPN_BISECTION_STEPS; i++) {
            double mid = 0.5 * (lo + hi);
            if (zpnRadial(wcsHeader.pv2, mid) < target)
                lo = mid;
            else
                hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    private static double clampZpnRadial(double radial, double lo, double hi) {
        if (lo > hi)
            return lo;
        return Math.clamp(radial, lo, hi);
    }

    static double zpnPrimaryBranchUpperEta(float[] pv2) {
        double maxEta = Math.PI;
        double prevEta = 0;
        if (zpnDerivative(pv2, prevEta) <= 0)
            return 0;

        for (int i = 1; i <= 512; i++) {
            double eta = maxEta * i / 512.;
            double derivative = zpnDerivative(pv2, eta);
            if (derivative <= 0) {
                double lo = prevEta;
                double hi = eta;
                for (int j = 0; j < ZPN_BISECTION_STEPS; j++) {
                    double mid = 0.5 * (lo + hi);
                    if (zpnDerivative(pv2, mid) > 0)
                        lo = mid;
                    else
                        hi = mid;
                }
                return 0.5 * (lo + hi);
            }
            prevEta = eta;
        }
        return maxEta;
    }

    static double zpnRadial(float[] pv2, double eta) {
        double radial = pv2[pv2.length - 1];
        for (int i = pv2.length - 2; i >= 0; i--)
            radial = radial * eta + pv2[i];
        return radial;
    }

    private static double zpnDerivative(float[] pv2, double eta) {
        double derivative = pv2.length - 1;
        derivative *= pv2[pv2.length - 1];
        for (int i = pv2.length - 2; i >= 1; i--)
            derivative = derivative * eta + i * pv2[i];
        return derivative;
    }
}
