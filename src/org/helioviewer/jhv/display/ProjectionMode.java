package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
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
    Orthographic(GLSLSolarShader.ortho, GridScale.ortho, NonOrthoKind.NONE) {
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
        public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color, double size, double radius) {
            vexBuf.putVertex((float) (vertex.x * radius), (float) (vertex.y * radius), (float) (vertex.z * radius), (float) size, color);
        }

        @Override
        public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
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

            double theta = Math.toDegrees(SphericalCoords.latitude(p));
            double phi = Math.toDegrees(SphericalCoords.longitude(p));

            if (gridType == GridType.Carrington && phi < 0)
                phi += 360;
            return new Vec2(phi, theta);
        }
    },
    HPC(GLSLSolarShader.hpc, GridScale.hpc, NonOrthoKind.HPC),
    Latitudinal(GLSLSolarShader.lati, GridScale.lati, NonOrthoKind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar, NonOrthoKind.POLAR),
    Polar(GLSLSolarShader.polar, GridScale.polar, NonOrthoKind.POLAR);

    enum NonOrthoKind {
        NONE,
        HPC,
        LATITUDINAL,
        POLAR
    }

    public final GLSLSolarShader shader;
    public final GridScale scale;
    private final NonOrthoKind nonOrthoKind;

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale, NonOrthoKind _nonOrthoKind) {
        shader = _shader;
        scale = _scale;
        nonOrthoKind = _nonOrthoKind;
    }

    public final Vec2 project(Position viewpoint, GridType gridType, Vec3 v) {
        return projectMap(viewpoint, gridType, v);
    }

    public final Vec3 unproject(Position viewpoint, GridType gridType, Vec2 pt) {
        return unprojectMap(viewpoint, gridType, pt);
    }

    public final Vec2 projectToScreen(Position viewpoint, GridType gridType, Viewport vp, Vec3 v) {
        return NonOrthoProjection.projectToScreen(nonOrthoKind, viewpoint, gridType, scale, vp, v);
    }

    private Vec2 projectMap(Position viewpoint, GridType gridType, Vec3 v) {
        return NonOrthoProjection.project(nonOrthoKind, viewpoint, gridType, v, scale);
    }

    private Vec3 unprojectMap(Position viewpoint, GridType gridType, Vec2 pt) {
        return NonOrthoProjection.unproject(nonOrthoKind, viewpoint, gridType, pt);
    }

    public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        return NonOrthoProjection.emitMapVertex(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, previous, vexBuf, color, first, last);
    }

    public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color, double size, double radius) {
        NonOrthoProjection.emitMapPoint(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, vexBuf, color, size);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        Vec2 pt = mouseToScreen(camera, vp, x, y);
        return new Vec2(
                scale.getInterpolatedXDisplayValue(pt.x + 0.5, gridType),
                scale.getInterpolatedYValue(pt.y + 0.5));
    }

    public Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y) {
        double gx = CameraHelper.computeUpX(camera, vp, x) / vp.aspect;
        double gy = CameraHelper.computeUpY(camera, vp, y);
        return new Vec2(gx, gy);
    }

    public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return unproject(camera.getViewpoint(), gridType, mouseToGrid(camera, vp, x, y, gridType));
    }

    public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) { // doesn't work well for Lati/*Polar
        return NonOrthoProjection.unprojectDisplayPoint(nonOrthoKind, camera, vp, x, y);
    }
}
