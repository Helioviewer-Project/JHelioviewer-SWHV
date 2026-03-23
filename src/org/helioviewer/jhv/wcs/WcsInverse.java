package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;

final class WcsInverse {

    private WcsInverse() {
    }

    static Vec2 inverseWcsPlaneToHelioprojective(WcsProjection.Context context, double planeX, double planeY) {
        if (context.projection == MetaData.WCSProjection.AZP)
            return inverseAzpToHelioprojective(context, planeX, planeY);
        if (context.projection == MetaData.WCSProjection.ZPN)
            return inverseZpnToHelioprojective(context, planeX, planeY);
        return inverseTanToHelioprojective(context, planeX, planeY);
    }

    private static Vec2 inverseTanToHelioprojective(WcsProjection.Context context, double planeX, double planeY) {
        double x = planeX / context.unitsPerRad;
        double y = planeY / context.unitsPerRad;
        double rho = Math.sqrt(x * x + y * y);
        if (rho == 0)
            return new Vec2(context.phi0, context.theta0);

        double cosNativeDistance = 1 / Math.sqrt(1 + rho * rho);
        double nativeX = x * cosNativeDistance;
        double nativeY = y * cosNativeDistance;
        return nativeToHelioprojective(context, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 inverseAzpToHelioprojective(WcsProjection.Context context, double planeX, double planeY) {
        double x = planeX / context.unitsPerRad;
        double y = planeY / context.unitsPerRad;
        double radial = Math.sqrt(x * x + y * y);
        if (radial == 0)
            return new Vec2(context.phi0, context.theta0);

        double mu = context.pv2[1];
        double gamma = Math.toRadians(context.pv2[2]);
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
        return nativeToHelioprojective(context, nativeX, nativeY, cosNativeDistance);
    }

    private static Vec2 nativeToHelioprojective(WcsProjection.Context context, double nativeX, double nativeY, double cosNativeDistance) {
        return new Vec2(
                context.phi0 + Math.atan2(nativeX, cosNativeDistance * Math.cos(context.theta0) - nativeY * Math.sin(context.theta0)),
                Math.asin(cosNativeDistance * Math.sin(context.theta0) + nativeY * Math.cos(context.theta0)));
    }

    private static Vec2 inverseZpnToHelioprojective(WcsProjection.Context context, double planeX, double planeY) {
        double x = planeX / context.unitsPerRad;
        double y = planeY / context.unitsPerRad;
        double radial = Math.sqrt(x * x + y * y);
        double nativeDistance = inverseZpnPrimaryBranch(context.pv2, radial);
        if (nativeDistance == 0)
            return new Vec2(context.phi0, context.theta0);

        double nativeRadius = Math.sin(nativeDistance);
        double nativeX = nativeRadius * x / radial;
        double nativeY = nativeRadius * y / radial;
        double cosNativeDistance = Math.cos(nativeDistance);
        return nativeToHelioprojective(context, nativeX, nativeY, cosNativeDistance);
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
