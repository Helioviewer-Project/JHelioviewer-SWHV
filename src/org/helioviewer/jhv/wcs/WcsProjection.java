package org.helioviewer.jhv.wcs;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public final class WcsProjection {

    private static final int ZPN_BISECTION_STEPS = 50;

    public static Vec2 planeToHelioprojective(WcsHeader wcsHeader, double x, double y) {
        Quat crota = wcsHeader.crota;
        double vx = crota.w * x - crota.z * y;
        double vy = crota.z * x + crota.w * y;
        double vz = crota.x * y - crota.y * x;
        double planeX = (vz * crota.y - vy * crota.z) * 2 + x;
        double planeY = (vx * crota.z - vz * crota.x) * 2 + y;
        return inverseWcsPlaneToHelioprojective(wcsHeader, planeX, planeY);
    }

    @Nullable
    public static Vec2 helioprojectiveToPlane(WcsHeader wcsHeader, double longitude, double latitude) {
        Vec2 plane = helioprojectiveToWcsPlane(wcsHeader, longitude, latitude);
        if (plane == null)
            return null;
        Vec3 rotated = wcsHeader.crota.rotateInverseVector(new Vec3(plane.x, plane.y, 0));
        return new Vec2(rotated.x, rotated.y);
    }

    public static Vec3 helioprojectiveToWorld(Position viewpoint, double longitude, double latitude) {
        Vec3 ray = helioprojectiveRay(longitude, latitude);

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

    private static Vec3 helioprojectiveRay(double longitude, double latitude) {
        double cosLon = Math.cos(longitude);
        double cosLat = Math.cos(latitude);
        double sign = cosLon * cosLat < 0 ? -1 : 1;
        return new Vec3(sign * Math.sin(longitude) * cosLat, sign * Math.sin(latitude), -sign * cosLon * cosLat);
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

    @Nullable
    private static Vec2 helioprojectiveToWcsPlane(WcsHeader wcsHeader, double longitude, double latitude) {
        double dphi = longitude - wcsHeader.phi0;
        double sinLat = Math.sin(latitude);
        double cosLat = Math.cos(latitude);
        double sinDphi = Math.sin(dphi);
        double cosDphi = Math.cos(dphi);
        double nativeX = cosLat * sinDphi;
        double nativeY = sinLat * wcsHeader.cosTheta0 - cosLat * wcsHeader.sinTheta0 * cosDphi;
        double cosNativeDistance = sinLat * wcsHeader.sinTheta0 + cosLat * wcsHeader.cosTheta0 * cosDphi;
        return nativeToWcsPlane(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    @Nullable
    private static Vec2 nativeToWcsPlane(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        if (wcsHeader.projection == WcsHeader.Projection.ARC)
            return nativeToArc(wcsHeader, nativeX, nativeY, cosNativeDistance);
        if (wcsHeader.projection == WcsHeader.Projection.AZP)
            return nativeToAzp(wcsHeader, nativeX, nativeY, cosNativeDistance);
        if (wcsHeader.projection == WcsHeader.Projection.ZPN)
            return nativeToZpn(wcsHeader, nativeX, nativeY, cosNativeDistance);
        return nativeToTan(wcsHeader, nativeX, nativeY, cosNativeDistance);
    }

    @Nullable
    private static Vec2 nativeToTan(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        if (cosNativeDistance <= 0)
            return null;
        return new Vec2(
                wcsHeader.unitsPerRad * nativeX / cosNativeDistance,
                wcsHeader.unitsPerRad * nativeY / cosNativeDistance);
    }

    private static Vec2 nativeToArc(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        double nativeRadius = Math.hypot(nativeX, nativeY);
        if (nativeRadius == 0)
            return new Vec2(0, 0);

        double nativeDistance = Math.atan2(nativeRadius, cosNativeDistance);
        double radial = wcsHeader.unitsPerRad * nativeDistance;
        return new Vec2(radial * nativeX / nativeRadius, radial * nativeY / nativeRadius);
    }

    @Nullable
    private static Vec2 nativeToAzp(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        double nativeRadius = Math.hypot(nativeX, nativeY);
        if (nativeRadius == 0)
            return new Vec2(0, 0);

        double mu = wcsHeader.azpMu;
        if (wcsHeader.azpSinGamma == 0 && mu > 1 && mu * cosNativeDistance + 1 <= 0)
            return null;

        double denom = wcsHeader.azpCosGamma * (wcsHeader.azpMu + cosNativeDistance) - nativeY * wcsHeader.azpSinGamma;
        if (denom <= 0)
            return null;

        double scale = wcsHeader.unitsPerRad * wcsHeader.azpMuPlus1 / denom;
        return new Vec2(wcsHeader.azpCosGamma * nativeX * scale, nativeY * scale);
    }

    @Nullable
    private static Vec2 nativeToZpn(WcsHeader wcsHeader, double nativeX, double nativeY, double cosNativeDistance) {
        double nativeRadius = Math.hypot(nativeX, nativeY);
        if (nativeRadius == 0)
            return new Vec2(0, 0);

        double nativeDistance = Math.atan2(nativeRadius, cosNativeDistance);
        if (nativeDistance > wcsHeader.zpnUpperEta)
            return null;

        double radial = wcsHeader.unitsPerRad * zpnRadial(wcsHeader.pv2, nativeDistance);
        if (radial < 0)
            return null;

        return new Vec2(radial * nativeX / nativeRadius, radial * nativeY / nativeRadius);
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
        if (radial == 0)
            return new Vec2(wcsHeader.phi0, wcsHeader.theta0);

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

    private WcsProjection() {}
}
