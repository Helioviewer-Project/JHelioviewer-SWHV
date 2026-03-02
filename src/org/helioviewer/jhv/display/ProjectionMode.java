package org.helioviewer.jhv.display;

import javax.swing.JRadioButtonMenuItem;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, GridScale.ortho) {
        @Override
        public Vec2 transform(Quat q, Vec3 v) {
            Vec3 rotated = q.rotateVector(v);
            return new Vec2(rotated.x, -rotated.y);
        }

        @Override
        public Vec3 transformInverse(Quat q, Vec2 pt) {
            double x = pt.x;
            double y = -pt.y;
            double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
            return q.rotateInverseVector(new Vec3(x, y, z));
        }
    },
    Latitudinal(GLSLSolarShader.lati, GridScale.lati) {
        @Override
        public Vec2 transform(Quat q, Vec3 v) {
            return transformLatitudinal(q.rotateVector(v), scale);
        }

        @Override
        public Vec3 transformInverse(Quat q, Vec2 pt) {
            return q.rotateInverseVector(transformInverseLatitudinal(pt));
        }
    },
    LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar) {
        @Override
        public Vec2 transform(Quat q, Vec3 v) {
            return transformPolar(q.rotateVector(v), scale);
        }

        @Override
        public Vec3 transformInverse(Quat q, Vec2 pt) {
            return q.rotateInverseVector(transformInversePolar(pt));
        }
    },
    Polar(GLSLSolarShader.polar, GridScale.polar) {
        @Override
        public Vec2 transform(Quat q, Vec3 v) {
            return transformPolar(q.rotateVector(v), scale);
        }

        @Override
        public Vec3 transformInverse(Quat q, Vec2 pt) {
            return q.rotateInverseVector(transformInversePolar(pt));
        }
    };

    public final GLSLSolarShader shader;
    public final GridScale scale;
    public final JRadioButtonMenuItem radio;

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale) {
        shader = _shader;
        scale = _scale;
        radio = new JRadioButtonMenuItem(toString());
        radio.addActionListener(e -> Display.setProjectionMode(this));
    }

    public abstract Vec2 transform(Quat q, Vec3 v);

    public abstract Vec3 transformInverse(Quat q, Vec2 pt);

    private static Vec2 transformPolar(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = Math.atan2(-v.x, -v.y);
        theta += 2 * Math.PI;
        theta %= 2 * Math.PI;
        double scaledr = scale.getYValueInv(r);
        double scaledtheta = scale.getXValueInv(Math.toDegrees(theta));
        return new Vec2(scaledtheta, scaledr);
    }

    private static Vec3 transformInversePolar(Vec2 pt) {
        double r = pt.y;
        double theta = -Math.toRadians(pt.x);
        double y = r * Math.cos(theta);
        double x = r * Math.sin(theta);
        double z = Math.sqrt(Math.max(0, 1 - x * x - y * y));
        return new Vec3(x, y, z);
    }

    private static Vec2 transformLatitudinal(Vec3 v, GridScale scale) {
        double theta = Math.asin(Math.clamp(-v.y, -1., 1.));
        double phi = Math.atan2(v.x, v.z);
        double scaledphi = scale.getXValueInv(Math.toDegrees(phi));
        double scaledtheta = scale.getYValueInv(Math.toDegrees(theta));
        return new Vec2(scaledphi, scaledtheta);
    }

    private static Vec3 transformInverseLatitudinal(Vec2 pt) {
        double phi = Math.toRadians(pt.x);
        double theta = Math.toRadians(pt.y);
        return new Vec3(Math.cos(theta) * Math.sin(phi), Math.sin(theta), Math.cos(theta) * Math.cos(phi));
    }
}
