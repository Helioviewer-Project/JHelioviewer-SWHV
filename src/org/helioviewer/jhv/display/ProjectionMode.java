package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
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

        @Override
        public Vec2 drawProjectedVertex(Quat q, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
            if (first) {
                vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
            }
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, color);
            if (last) {
                vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), 1, Colors.Null);
            }
            return previous;
        }

        @Override
        public Position projectionViewpoint(Position viewpoint) {
            return viewpoint;
        }

        @Override
        public Quat mouseRotation(Camera camera, GridType gridType) {
            return camera.getViewpoint().toQuat();
        }

        @Override
        public Vec3 unprojectMousePoint(Camera camera, Viewport vp, double x, double y, Quat rotation, GridType gridType, boolean correctDrag) {
            return CameraHelper.getVectorFromSphere(camera, vp, x, y, rotation, correctDrag);
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

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale) {
        shader = _shader;
        scale = _scale;
    }

    public abstract Vec2 transform(Quat q, Vec3 v);

    public abstract Vec3 transformInverse(Quat q, Vec2 pt);

    public Vec2 drawProjectedVertex(Quat q, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        Vec3 projected = new Vec3(vertex.x, -vertex.y, vertex.z);
        if (first)
            GLHelper.drawVertex(q, vp, projected, previous, vexBuf, Colors.Null);
        Vec2 current = GLHelper.drawVertex(q, vp, projected, previous, vexBuf, color);
        if (last)
            GLHelper.drawVertex(q, vp, projected, current, vexBuf, Colors.Null);
        return current;
    }

    public Position projectionViewpoint(Position viewpoint) {
        return new Position(viewpoint.time, Sun.MeanEarthDistance, viewpoint.lon, viewpoint.lat);
    }

    public Quat mouseRotation(Camera camera, GridType gridType) {
        return gridType.toCarrington(camera.getViewpoint());
    }

    public Vec3 unprojectMousePoint(Camera camera, Viewport vp, double x, double y, Quat rotation, GridType gridType, boolean correctDrag) {
        return transformInverse(rotation, scale.mouseToGrid((int) x, (int) y, vp, camera, gridType));
    }

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
