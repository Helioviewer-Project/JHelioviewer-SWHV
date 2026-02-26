package org.helioviewer.jhv.display;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

interface GridTransform {

    Vec2 transform(Vec3 v, GridScale scale);

    Vec3 transformInverse(Vec2 pt);

    GridTransform xformPolar = new GridTransformPolar();
    GridTransform xformLati = new GridTransformLatitudinal();

    class GridTransformPolar implements GridTransform {
        @Override
        public Vec2 transform(Vec3 v, GridScale scale) {
            double r = Math.sqrt(v.x * v.x + v.y * v.y);
            double theta = Math.atan2(-v.x, -v.y);
            theta += 2 * Math.PI;
            theta %= 2 * Math.PI;
            double scaledr = scale.getYValueInv(r);
            double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
            return new Vec2(scaledtheta, scaledr);
        }

        @Override
        public Vec3 transformInverse(Vec2 pt) {
            double r = pt.y;
            double theta = -Math.toRadians(pt.x);
            double y = r * Math.cos(theta);
            double x = r * Math.sin(theta);
            double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
            return new Vec3(x, y, z);
        }
    }

    class GridTransformLatitudinal implements GridTransform {
        @Override
        public Vec2 transform(Vec3 v, GridScale scale) {
            double theta = Math.asin(Math.clamp(-v.y, -1., 1.));
            double phi = Math.atan2(v.x, v.z);
            double scaledphi = scale.getXValueInv(Math.toDegrees(phi));
            double scaledtheta = scale.getYValueInv(Math.toDegrees(theta));
            return new Vec2(scaledphi, scaledtheta);
        }

        @Override
        public Vec3 transformInverse(Vec2 pt) {
            double phi = Math.toRadians(pt.x);
            double theta = Math.toRadians(pt.y);
            return new Vec3(Math.cos(theta) * Math.sin(phi), Math.sin(theta), Math.cos(theta) * Math.cos(phi));
        }
    }

}
