package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public interface Transform {

    Vec2 transform(Vec3 pt, GridScale scale);
    Vec3 transformInverse(Vec2 pt);

    Transform transformpolar = new TransformPolar();
    Transform transformlatitudinal = new TransformLatitudinal();

    class TransformPolar implements Transform {
        @Override
        public Vec2 transform(Vec3 pt, GridScale scale) {
            Position.L p = Sun.getEarth(Movie.getTime());
            Quat q = new Quat(p.lat, 0);
            pt = q.rotateInverseVector(pt);
            double r = Math.sqrt(pt.x * pt.x + pt.y * pt.y);
            double theta = Math.atan2(-pt.x, -pt.y);
            theta += 2 * Math.PI;
            theta %= 2 * Math.PI;
            double scaledr = scale.getYValueInv(r);
            double scaledtheta = scale.getXValueInv(theta * MathUtils.radeg);
            return new Vec2(scaledtheta, scaledr);
        }

        @Override
        public Vec3 transformInverse(Vec2 pt) {
            double r = pt.y;
            double theta = -pt.x / MathUtils.radeg;
            double y = r * Math.cos(theta);
            double x = r * Math.sin(theta);
            double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
            Quat q = Sun.getEarthQuat(Movie.getTime()).orientation;
            return q.rotateInverseVector(new Vec3(x, y, z));
        }
    }

    class TransformLatitudinal implements Transform {
        @Override
        public Vec2 transform(Vec3 pt, GridScale scale) {
            double theta = Math.PI / 2 - Math.acos(-pt.y);
            double phi = Math.atan2(pt.x, pt.z);
            Position.L p = Sun.getEarth(Movie.getTime());
            phi -= Math.PI + p.lon;
            phi += 6 * Math.PI;
            phi %= 2 * Math.PI;
            double scaledphi = scale.getXValueInv(phi * MathUtils.radeg);
            double scaledtheta = scale.getYValueInv(theta * MathUtils.radeg);
            return new Vec2(scaledphi, scaledtheta);
        }

        @Override
        public Vec3 transformInverse(Vec2 pt) {
            double phi = pt.x / MathUtils.radeg;
            double theta = pt.y / MathUtils.radeg;
            phi += Math.PI;
            theta = -theta + Math.PI / 2;
            return new Vec3(Math.sin(theta) * Math.sin(phi), Math.cos(theta), Math.sin(theta) * Math.cos(phi));
        }
    }

}
