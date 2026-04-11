package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public final class WcsProjection {

    private WcsProjection() {
    }

    public static Vec2 planeToHelioprojective(WcsHeader wcsHeader, double x, double y) {
        Vec3 plane = wcsHeader.crota.rotateVector(new Vec3(x, y, 0));
        return inverseWcsPlaneToHelioprojective(wcsHeader, plane.x, plane.y);
    }

    private static Vec2 inverseWcsPlaneToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        if (wcsHeader.projection == WcsHeader.Projection.AZP)
            return inverseAzpToHelioprojective(wcsHeader, planeX, planeY);
        if (wcsHeader.projection == WcsHeader.Projection.ZPN)
            return inverseZpnToHelioprojective(wcsHeader, planeX, planeY);
        return inverseTanToHelioprojective(wcsHeader, planeX, planeY);
    }

    private static Vec2 inverseTanToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX / wcsHeader.unitsPerRad;
        double y = planeY / wcsHeader.unitsPerRad;
        double rho = Math.sqrt(x * x + y * y);
        if (rho == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double cosNativeDistance = 1 / Math.sqrt(1 + rho * rho);
        double nativeX = x * cosNativeDistance;
        double nativeY = y * cosNativeDistance;
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 inverseAzpToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX / wcsHeader.unitsPerRad;
        double y = planeY / wcsHeader.unitsPerRad;
        double radial = Math.sqrt(x * x + y * y);
        if (radial == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double mu = wcsHeader.pv2[1];
        double gamma = Math.toRadians(wcsHeader.pv2[2]);
        double sinGamma = Math.sin(gamma);
        double cosGamma = Math.cos(gamma);
        double muPlus1 = mu + 1;
        double a = 1 + y * sinGamma / muPlus1;
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
                wcsHeader.phi0 + Math.atan2(nativeX, cosNativeDistance * Math.cos(wcsHeader.theta0) - nativeY * Math.sin(wcsHeader.theta0)),
                Math.asin(cosNativeDistance * Math.sin(wcsHeader.theta0) + nativeY * Math.cos(wcsHeader.theta0)));
    }

    private static Vec2 inverseZpnToHelioprojective(WcsHeader wcsHeader, double planeX, double planeY) {
        double x = planeX / wcsHeader.unitsPerRad;
        double y = planeY / wcsHeader.unitsPerRad;
        double radial = Math.sqrt(x * x + y * y);
        double nativeDistance = inverseZpnPrimaryBranch(wcsHeader.pv2, radial);
        if (nativeDistance == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

        double nativeRadius = Math.sin(nativeDistance);
        double nativeX = nativeRadius * x / radial;
        double nativeY = nativeRadius * y / radial;
        double cosNativeDistance = Math.cos(nativeDistance);
        return nativeToHelioprojective(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    private static double inverseZpnPrimaryBranch(float[] pv2, double radial) {
        double upper = zpnPrimaryBranchUpperEta(pv2);
        double lo = 0;
        double hi = upper;
        double target = Math.clamp(radial, zpnRadialAndDerivative(pv2, lo).radial(), zpnRadialAndDerivative(pv2, hi).radial());
        for (int i = 0; i < 64; i++) {
            double mid = 0.5 * (lo + hi);
            if (zpnRadialAndDerivative(pv2, mid).radial() < target)
                lo = mid;
            else
                hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    private static double zpnPrimaryBranchUpperEta(float[] pv2) {
        double maxEta = Math.PI;
        double prevEta = 0;
        if (zpnRadialAndDerivative(pv2, prevEta).derivative() <= 0)
            return 0;

        for (int i = 1; i <= 512; i++) {
            double eta = maxEta * i / 512.;
            double derivative = zpnRadialAndDerivative(pv2, eta).derivative();
            if (derivative <= 0) {
                double lo = prevEta;
                double hi = eta;
                for (int j = 0; j < 64; j++) {
                    double mid = 0.5 * (lo + hi);
                    if (zpnRadialAndDerivative(pv2, mid).derivative() > 0)
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

    private record RadialAndDerivative(double radial, double derivative) {
    }

    private static RadialAndDerivative zpnRadialAndDerivative(float[] pv2, double eta) {
        double radial = pv2[pv2.length - 1];
        double derivative = pv2.length - 1;
        derivative *= pv2[pv2.length - 1];
        for (int i = pv2.length - 2; i >= 1; i--) {
            radial = radial * eta + pv2[i];
            derivative = derivative * eta + i * pv2[i];
        }
        radial = radial * eta + pv2[0];
        return new RadialAndDerivative(radial, derivative);
    }
}
