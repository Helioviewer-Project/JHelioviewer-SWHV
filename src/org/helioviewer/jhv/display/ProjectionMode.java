package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, GridScale.ortho) {
        @Override
        protected Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not use project()");
        }

        @Override
        protected Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
            throw new UnsupportedOperationException("Orthographic mode does not use unproject()");
        }

        @Override
        public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
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
        public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return CameraHelper.unprojectToOutputSphere(camera, vp, x, y, camera.getViewpoint().toQuat());
        }

        @Override
        public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return CameraHelper.unprojectToCurrentViewSphereOrPlane(camera, vp, x, y);
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

            double theta = Math.toDegrees(SphericalCoords.latitude(p));
            double phi = Math.toDegrees(SphericalCoords.longitude(p));

            if (gridType == GridType.Carrington && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }
    },
    HPC(GLSLSolarShader.hpc, GridScale.hpc) {
        @Override
        protected Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
            return NonOrthoProjection.projectHpc(viewpoint, v, scale);
        }

        @Override
        protected Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
            return NonOrthoProjection.unprojectHpc(viewpoint, pt);
        }

        @Override
        public Position projectionViewpoint(Position viewpoint) {
            return viewpoint;
        }

        @Override
        public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return new Vec3(
                    CameraHelper.computeUpX(camera, vp, x),
                    CameraHelper.computeUpY(camera, vp, y),
                    0);
        }
    },
    Latitudinal(GLSLSolarShader.lati, GridScale.lati) {
        @Override
        protected Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
            return NonOrthoProjection.projectLatitudinal(viewpoint, gridType, v, scale);
        }

        @Override
        protected Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
            return NonOrthoProjection.unprojectLatitudinal(viewpoint, gridType, pt);
        }
    },
    LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar) {
        @Override
        protected Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
            return NonOrthoProjection.projectPolar(viewpoint, gridType, v, scale);
        }

        @Override
        protected Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
            return NonOrthoProjection.unprojectPolar(viewpoint, gridType, pt);
        }
    },
    Polar(GLSLSolarShader.polar, GridScale.polar) {
        @Override
        protected Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
            return NonOrthoProjection.projectPolar(viewpoint, gridType, v, scale);
        }

        @Override
        protected Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
            return NonOrthoProjection.unprojectPolar(viewpoint, gridType, pt);
        }
    };

    public final GLSLSolarShader shader;
    public final GridScale scale;

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale) {
        shader = _shader;
        scale = _scale;
    }

    public final Vec2 project(Position viewpoint, GridType gridType, Vec3 v) {
        return projectMap(viewpoint, gridType, v);
    }

    public final Vec3 unproject(Position viewpoint, GridType gridType, Vec2 pt) {
        return unprojectMap(viewpoint, gridType, pt);
    }

    public final Vec2 projectToScreenCoords(Position viewpoint, GridType gridType, Viewport vp, Vec3 v) {
        Vec2 projected = projectMap(viewpoint, gridType, v);
        return new Vec2(projected.x * vp.aspect, projected.y);
    }

    protected abstract Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v);

    protected abstract Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt);

    public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        Vec2 current = projectMap(viewpoint, gridType, vertex);
        if (first)
            emitProjectedVertex(vp, current, vexBuf, Colors.Null);
        emitWrappedVertex(vp, previous, current, vexBuf, color);
        if (last)
            emitProjectedVertex(vp, current, vexBuf, Colors.Null);
        return current;
    }

    private static void emitWrappedVertex(Viewport vp, Vec2 previous, Vec2 current, BufVertex vexBuf, byte[] color) {
        if (previous != null && Math.abs(previous.x - current.x) > 0.5) {
            emitHorizontalWrap(vp, current, previous, vexBuf, color);
        }
        emitProjectedVertex(vp, current, vexBuf, color);
    }

    private static void emitHorizontalWrap(Viewport vp, Vec2 current, Vec2 previous, BufVertex vexBuf, byte[] color) {
        float y = (float) current.y;
        float x;
        if (current.x <= 0 && previous.x >= 0) {
            x = (float) (0.5 * vp.aspect);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.putVertex(x, y, 0, 1, Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.putVertex(-x, y, 0, 1, color);
        } else if (current.x >= 0 && previous.x <= 0) {
            x = (float) (-0.5 * vp.aspect);
            vexBuf.putVertex(x, y, 0, 1, color);
            vexBuf.putVertex(x, y, 0, 1, Colors.Null);

            vexBuf.putVertex(-x, y, 0, 1, Colors.Null);
            vexBuf.putVertex(-x, y, 0, 1, color);
        }
    }

    private static void emitProjectedVertex(Viewport vp, Vec2 projected, BufVertex vexBuf, byte[] color) {
        vexBuf.putVertex((float) (projected.x * vp.aspect), (float) projected.y, 0, 1, color);
    }

    public Position projectionViewpoint(Position viewpoint) {
        return new Position(viewpoint.time, Sun.MeanEarthDistance, viewpoint.lon, viewpoint.lat);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Vec2 pt = mouseToViewPlane(camera, vp, x, y);
        return new Vec2(
                scale.getInterpolatedXDisplayValue(pt.x + 0.5, gridType),
                scale.getInterpolatedYValue(pt.y + 0.5));
    }

    public Vec2 mouseToViewPlane(Camera camera, Viewport vp, int x, int y) {
        double gx = CameraHelper.computeUpX(camera, vp, x) / vp.aspect;
        double gy = CameraHelper.computeUpY(camera, vp, y);
        return new Vec2(gx, gy);
    }

    public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return unproject(camera.getViewpoint(), gridType, mouseToGrid(camera, vp, x, y, gridType));
    }

    public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return null;
    }
}
