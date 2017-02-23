package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.layers.Layers;
import org.jetbrains.annotations.NotNull;

public interface Transform {

    @NotNull Vec2 transform(@NotNull Vec3 pt, @NotNull GridScale scale);
    @NotNull Vec3 transformInverse(@NotNull Vec2 pt);

    Transform transformpolar = new TransformPolar();
    Transform transformlatitudinal = new TransformLatitudinal();

    class TransformPolar implements Transform {
        @NotNull
        @Override
        public Vec2 transform(@NotNull Vec3 pt, @NotNull GridScale scale) {
            Position.L p = Sun.getEarth(Layers.getLastUpdatedTimestamp());
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

        @NotNull
        @Override
        public Vec3 transformInverse(@NotNull Vec2 pt) {
            double r = pt.y;
            double theta = -pt.x / MathUtils.radeg;
            double y = r * Math.cos(theta);
            double x = r * Math.sin(theta);
            double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
            Position.L p = Sun.getEarth(Layers.getLastUpdatedTimestamp());
            Quat q = new Quat(p.lat, p.lon);
            return q.rotateInverseVector(new Vec3(x, y, z));
        }
    }

    class TransformLatitudinal implements Transform {
        @NotNull
        @Override
        public Vec2 transform(@NotNull Vec3 pt, @NotNull GridScale scale) {
            double theta = Math.PI / 2 - Math.acos(-pt.y);
            double phi = Math.atan2(pt.x, pt.z);
            Position.L p = Sun.getEarth(Layers.getLastUpdatedTimestamp());
            phi -= Math.PI + p.lon;
            phi += 6 * Math.PI;
            phi %= 2 * Math.PI;
            double scaledphi = scale.getXValueInv(phi * MathUtils.radeg);
            double scaledtheta = scale.getYValueInv(theta * MathUtils.radeg);
            return new Vec2(scaledphi, scaledtheta);
        }

        @NotNull
        @Override
        public Vec3 transformInverse(@NotNull Vec2 pt) {
            double phi = pt.x / MathUtils.radeg;
            double theta = pt.y / MathUtils.radeg;
            phi += Math.PI;
            theta = -theta + Math.PI / 2;
            return new Vec3(Math.sin(theta) * Math.sin(phi), Math.cos(theta), Math.sin(theta) * Math.cos(phi));
        }
    }

}
