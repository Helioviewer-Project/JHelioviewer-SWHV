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
        protected Vec2 transformFlat(Position viewpoint, GridType gridType, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not use transform()");
        }

        @Override
        protected Vec3 transformInverseFlat(Position viewpoint, GridType gridType, Vec2 pt) {
            throw new UnsupportedOperationException("Orthographic mode does not use transformInverse()");
        }

        @Override
        public Vec2 drawProjectedVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
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
        public Vec3 unprojectMouse(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return CameraHelper.unprojectToOutputSphere(camera, vp, x, y, camera.getViewpoint().toQuat());
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            Quat rotation = Quat.ZERO; // final frame to unproject into
            if (gridType != GridType.Viewpoint) {
                Position viewpoint = camera.getViewpoint();
                rotation = Quat.rotateWithConjugate(viewpoint.toQuat(), gridType.toCarrington(viewpoint));
            }

            Vec3 p = CameraHelper.unprojectToOutputSphere(camera, vp, x, y, rotation);
            if (p == null)
                return Vec2.NAN;

            double theta = Math.toDegrees(Math.asin(Math.clamp(p.y, -1., 1.)));
            double phi = Math.toDegrees(Math.atan2(p.x, p.z));

            if (gridType == GridType.Carrington && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }
    },
    Latitudinal(GLSLSolarShader.lati, GridScale.lati) {
        @Override
        protected Vec2 transformFlat(Position viewpoint, GridType gridType, Vec3 v) {
            return transformLatitudinal(flatMapRotation(gridType, viewpoint).rotateVector(v), scale);
        }

        @Override
        protected Vec3 transformInverseFlat(Position viewpoint, GridType gridType, Vec2 pt) {
            return flatMapRotation(gridType, viewpoint).rotateInverseVector(transformInverseLatitudinal(pt));
        }
    },
    LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar) {
        @Override
        protected Vec2 transformFlat(Position viewpoint, GridType gridType, Vec3 v) {
            return transformPolar(flatMapRotation(gridType, viewpoint).rotateVector(v), scale);
        }

        @Override
        protected Vec3 transformInverseFlat(Position viewpoint, GridType gridType, Vec2 pt) {
            return flatMapRotation(gridType, viewpoint).rotateInverseVector(transformInversePolar(pt));
        }
    },
    Polar(GLSLSolarShader.polar, GridScale.polar) {
        @Override
        protected Vec2 transformFlat(Position viewpoint, GridType gridType, Vec3 v) {
            return transformPolar(flatMapRotation(gridType, viewpoint).rotateVector(v), scale);
        }

        @Override
        protected Vec3 transformInverseFlat(Position viewpoint, GridType gridType, Vec2 pt) {
            return flatMapRotation(gridType, viewpoint).rotateInverseVector(transformInversePolar(pt));
        }
    };

    public final GLSLSolarShader shader;
    public final GridScale scale;

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale) {
        shader = _shader;
        scale = _scale;
    }

    public final Vec2 transform(Position viewpoint, GridType gridType, Vec3 v) {
        return transformFlat(viewpoint, gridType, v);
    }

    public final Vec3 transformInverse(Position viewpoint, GridType gridType, Vec2 pt) {
        return transformInverseFlat(viewpoint, gridType, pt);
    }

    protected abstract Vec2 transformFlat(Position viewpoint, GridType gridType, Vec3 v);

    protected abstract Vec3 transformInverseFlat(Position viewpoint, GridType gridType, Vec2 pt);

    public Vec2 drawProjectedVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        if (first)
            GLHelper.drawVertex(viewpoint, gridType, vp, vertex, previous, vexBuf, Colors.Null);
        Vec2 current = GLHelper.drawVertex(viewpoint, gridType, vp, vertex, previous, vexBuf, color);
        if (last)
            GLHelper.drawVertex(viewpoint, gridType, vp, vertex, current, vexBuf, Colors.Null);
        return current;
    }

    private static Quat flatMapRotation(GridType gridType, Position viewpoint) {
        // Non-ortho maps use the same longitude as GridType.toGrid(), but the reflected
        // flat-map basis makes the effective viewpoint latitude rotation positive.
        return Quat.createXY(gridType == GridType.Viewpoint ? viewpoint.lat : 0, gridType.toLongitude(viewpoint));
    }

    public Position projectionViewpoint(Position viewpoint) {
        return new Position(viewpoint.time, Sun.MeanEarthDistance, viewpoint.lon, viewpoint.lat);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Vec2 pt = mouseToViewPlane(camera, vp, x, y);
        return new Vec2(
                scale.getInterpolatedXValue(pt.x + 0.5, gridType),
                scale.getInterpolatedYValue(pt.y + 0.5));
    }

    public Vec2 mouseToViewPlane(Camera camera, Viewport vp, int x, int y) {
        double gx = CameraHelper.computeUpX(camera, vp, x) / vp.aspect;
        double gy = CameraHelper.computeUpY(camera, vp, y);
        return new Vec2(gx, gy);
    }

    public Vec3 unprojectMouse(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return transformInverse(camera.getViewpoint(), gridType, mouseToGrid(camera, vp, x, y, gridType));
    }

    private static Vec2 transformPolar(Vec3 v, GridScale scale) {
        double r = Math.sqrt(v.x * v.x + v.y * v.y);
        double theta = Math.atan2(-v.x, v.y);
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
        double theta = Math.asin(Math.clamp(v.y, -1., 1.));
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
