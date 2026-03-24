package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, GridScale.ortho) {
        @Override
        public Vec2 projectToScreen(Position viewpoint, GridType gridType, Viewport vp, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not use projectToScreen()");
        }

        @Override
        public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return OrthoProjection.unprojectSurfacePoint(camera, vp, x, y);
        }

        @Override
        public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, byte[] color, boolean first, boolean last, double radius, BufVertex vexBuf) {
            OrthoProjection.emitMapVertex(vertex, color, first, last, radius, vexBuf);
            return previous;
        }

        @Override
        public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, byte[] color, double size, double radius, BufVertex vexBuf) {
            OrthoProjection.emitMapPoint(vertex, color, size, radius, vexBuf);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return OrthoProjection.mouseToGrid(camera, vp, x, y, gridType);
        }

        @Override
        public Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            throw new UnsupportedOperationException("Orthographic mode does not use non-ortho mouseToScreen()");
        }
    },
    HPC(GLSLSolarShader.hpc, GridScale.hpc, NonOrthoProjection.Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, GridScale.lati, NonOrthoProjection.Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, GridScale.logpolar, NonOrthoProjection.Kind.POLAR),
    Polar(GLSLSolarShader.polar, GridScale.polar, NonOrthoProjection.Kind.POLAR);

    public final GLSLSolarShader shader;
    public final GridScale scale;
    private final NonOrthoProjection.Kind nonOrthoKind;

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale) {
        this(_shader, _scale, null);
    }

    ProjectionMode(GLSLSolarShader _shader, GridScale _scale, NonOrthoProjection.Kind _nonOrthoKind) {
        shader = _shader;
        scale = _scale;
        nonOrthoKind = _nonOrthoKind;
    }

    public boolean isOrthographic() {
        return this == Orthographic;
    }

    public boolean isHpc() {
        return this == HPC;
    }

    public boolean isLatitudinal() {
        return this == Latitudinal;
    }

    public boolean isPolar() {
        return this == Polar;
    }

    public boolean isLogPolar() {
        return this == LogPolar;
    }

    public Vec2 projectToScreen(Position viewpoint, GridType gridType, Viewport vp, Vec3 v) {
        return NonOrthoProjection.projectToScreen(nonOrthoKind, viewpoint, gridType, scale, vp, v);
    }

    public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return NonOrthoProjection.unprojectSurfacePoint(nonOrthoKind, scale, camera, vp, x, y, gridType);
    }

    public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, byte[] color, boolean first, boolean last, double radius, BufVertex vexBuf) {
        return NonOrthoProjection.emitMapVertex(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, previous, color, first, last, vexBuf);
    }

    public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, byte[] color, double size, double radius, BufVertex vexBuf) {
        NonOrthoProjection.emitMapPoint(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, color, size, vexBuf);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return NonOrthoProjection.mouseToGrid(scale, camera, vp, x, y, gridType);
    }

    public Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return NonOrthoProjection.mouseToScreen(scale, camera, vp, x, y, gridType);
    }
}
