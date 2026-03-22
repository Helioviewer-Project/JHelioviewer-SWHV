package org.helioviewer.jhv.layers.image;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

public class HpcUtils {

    public static Region hpcBounds(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        HpcInverseContext context = new HpcInverseContext(metaData);
        double x0 = region.llx;
        double x1 = region.llx + region.width;
        double y0 = region.lly;
        double y1 = region.lly + region.height;
        double xm = 0.5 * (x0 + x1);
        double ym = 0.5 * (y0 + y1);
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double[] bounds = {minX, maxX, minY, maxY};
        updateHpcBounds(bounds, context, x0, y0);
        updateHpcBounds(bounds, context, x1, y0);
        updateHpcBounds(bounds, context, x0, y1);
        updateHpcBounds(bounds, context, x1, y1);
        updateHpcBounds(bounds, context, xm, y0);
        updateHpcBounds(bounds, context, xm, y1);
        updateHpcBounds(bounds, context, x0, ym);
        updateHpcBounds(bounds, context, x1, ym);
        minX = bounds[0];
        maxX = bounds[1];
        minY = bounds[2];
        maxY = bounds[3];
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    private static void updateHpcBounds(double[] bounds, HpcInverseContext context, double x, double y) {
        Vec3 plane = context.crota.rotateVector(new Vec3(x, y, 0));
        Vec2 helioprojective = inverseWcsPlaneToHpc(context, plane.x, plane.y);
        double hpcX = Math.toDegrees(helioprojective.x);
        double hpcY = Math.toDegrees(helioprojective.y);
        bounds[0] = Math.min(bounds[0], hpcX);
        bounds[1] = Math.max(bounds[1], hpcX);
        bounds[2] = Math.min(bounds[2], hpcY);
        bounds[3] = Math.max(bounds[3], hpcY);
    }

    private static Vec2 inverseWcsPlaneToHpc(HpcInverseContext context, double planeX, double planeY) {
        double unitsPerRad = context.unitsPerRad;
        double phi0 = context.phi0;
        double theta0 = context.theta0;

        if (context.projection == MetaData.WCSProjection.AZP && Math.abs(context.pv2[2]) < 1e-6f) {
            double x = planeX / unitsPerRad;
            double y = planeY / unitsPerRad;
            double r = Math.sqrt(x * x + y * y);
            if (r == 0)
                return new Vec2(phi0, theta0);

            double mu = context.pv2[1];
            double muPlus1 = mu + 1;
            double t;
            if (mu == 1) {
                t = 0.5 * r;
            } else {
                double discriminant = muPlus1 * muPlus1 - r * r * (mu * mu - 1);
                discriminant = Math.max(discriminant, 0);
                t = r * muPlus1 / (muPlus1 + Math.sqrt(discriminant));
            }
            double eta = 2 * Math.atan(t);
            double alpha = Math.atan2(x, y);
            double sinEta = Math.sin(eta);
            double cosEta = Math.cos(eta);
            double a = sinEta * Math.sin(alpha);
            double b = sinEta * Math.cos(alpha);
            return new Vec2(
                    phi0 + Math.atan2(a, cosEta * Math.cos(theta0) - b * Math.sin(theta0)),
                    Math.asin(cosEta * Math.sin(theta0) + b * Math.cos(theta0)));
        }

        if (context.projection == MetaData.WCSProjection.ZPN) {
            double x = planeX / unitsPerRad;
            double y = planeY / unitsPerRad;
            double radial = Math.sqrt(x * x + y * y);
            double eta = inverseZpnPrimaryBranch(context.pv2, radial);
            if (eta == 0)
                return new Vec2(phi0, theta0);

            double sinEta = Math.sin(eta);
            double cosEta = Math.cos(eta);
            double alpha = Math.atan2(x, y);
            double a = sinEta * Math.sin(alpha);
            double b = sinEta * Math.cos(alpha);
            return new Vec2(
                    phi0 + Math.atan2(a, cosEta * Math.cos(theta0) - b * Math.sin(theta0)),
                    Math.asin(cosEta * Math.sin(theta0) + b * Math.cos(theta0)));
        }

        double x = planeX / unitsPerRad;
        double y = planeY / unitsPerRad;
        double rho = Math.sqrt(x * x + y * y);
        if (rho == 0)
            return new Vec2(phi0, theta0);
        double c = Math.atan(rho);
        double sinc = Math.sin(c);
        double cosc = Math.cos(c);
        return new Vec2(
                phi0 + Math.atan2(x * sinc, rho * Math.cos(theta0) * cosc - y * Math.sin(theta0) * sinc),
                Math.asin(cosc * Math.sin(theta0) + y * sinc * Math.cos(theta0) / rho));
    }

    private static final class HpcInverseContext {
        private final Quat crota;
        private final MetaData.WCSProjection projection;
        private final float[] pv2;
        private final double unitsPerRad;
        private final double phi0;
        private final double theta0;

        private HpcInverseContext(MetaData metaData) {
            crota = metaData.getCROTA();
            projection = metaData.getWCSProjection();
            pv2 = metaData.getPV2();
            unitsPerRad = metaData.getWCSPlaneUnitsPerRad();
            Vec2 crval = metaData.getCRVAL();
            phi0 = crval.x / unitsPerRad;
            theta0 = crval.y / unitsPerRad;
        }
    }

    private static double inverseZpnPrimaryBranch(float[] pv2, double radial) {
        double upper = zpnPrimaryBranchUpperEta(pv2);
        double lo = 0;
        double hi = upper;
        double target = Math.clamp(radial, zpnRadial(pv2, lo), zpnRadial(pv2, hi));
        for (int i = 0; i < 64; i++) {
            double mid = 0.5 * (lo + hi);
            if (zpnRadial(pv2, mid) < target)
                lo = mid;
            else
                hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    private static double zpnPrimaryBranchUpperEta(float[] pv2) {
        double maxEta = Math.PI;
        double prevEta = 0;
        if (zpnRadialDerivative(pv2, prevEta) <= 0)
            return 0;

        for (int i = 1; i <= 512; i++) {
            double eta = maxEta * i / 512.;
            double derivative = zpnRadialDerivative(pv2, eta);
            if (derivative <= 0) {
                double lo = prevEta;
                double hi = eta;
                for (int j = 0; j < 64; j++) {
                    double mid = 0.5 * (lo + hi);
                    if (zpnRadialDerivative(pv2, mid) > 0)
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

    private static double zpnRadial(float[] pv2, double eta) {
        double radial = 0;
        double power = 1;
        for (float coefficient : pv2) {
            radial += coefficient * power;
            power *= eta;
        }
        return radial;
    }

    private static double zpnRadialDerivative(float[] pv2, double eta) {
        double derivative = 0;
        double power = 1;
        for (int i = 1; i < pv2.length; i++) {
            derivative += i * pv2[i] * power;
            power *= eta;
        }
        return derivative;
    }

}
