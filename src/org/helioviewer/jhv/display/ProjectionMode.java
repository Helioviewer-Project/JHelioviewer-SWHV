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
        public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
            OrthoProjection.emitMapVertex(vertex, vexBuf, color, first, last, radius);
            return previous;
        }

        @Override
        public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color, double size, double radius) {
            OrthoProjection.emitMapPoint(vertex, vexBuf, color, size, radius);
        }

        @Override
        public Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not use non-ortho mouseToScreen()");
        }

        @Override
        public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return OrthoProjection.unprojectSurfacePoint(camera, vp, x, y);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return OrthoProjection.mouseToGrid(camera, vp, x, y, gridType);
        }

        @Override
        public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
            return OrthoProjection.unprojectDisplayPoint(camera, vp, x, y);
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

    public boolean isPolarLike() {
        return this == Polar || this == LogPolar;
    }

    public Vec2 projectToScreen(Position viewpoint, GridType gridType, Viewport vp, Vec3 v) {
        return NonOrthoProjection.projectToScreen(nonOrthoKind, viewpoint, gridType, scale, vp, v);
    }

    public Vec2 emitMapVertex(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, Vec2 previous, BufVertex vexBuf, byte[] color, boolean first, boolean last, double radius) {
        return NonOrthoProjection.emitMapVertex(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, previous, vexBuf, color, first, last);
    }

    public void emitMapPoint(Position viewpoint, GridType gridType, Viewport vp, Vec3 vertex, BufVertex vexBuf, byte[] color, double size, double radius) {
        NonOrthoProjection.emitMapPoint(nonOrthoKind, viewpoint, gridType, scale, vp, vertex, vexBuf, color, size);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return NonOrthoProjection.mouseToGrid(scale, camera, vp, x, y, gridType);
    }

    public Vec2 mouseToScreen(Camera camera, Viewport vp, int x, int y) {
        return NonOrthoProjection.mouseToScreen(camera, vp, x, y);
    }

    public Vec3 unprojectSurfacePoint(Camera camera, Viewport vp, int x, int y, GridType gridType) {
        return NonOrthoProjection.unproject(nonOrthoKind, camera.getViewpoint(), gridType, mouseToGrid(camera, vp, x, y, gridType));
    }

    public Vec3 unprojectDisplayPoint(Camera camera, Viewport vp, int x, int y, GridType gridType) { // doesn't work well for Lati/*Polar
        return NonOrthoProjection.unprojectDisplayPoint(nonOrthoKind, camera, vp, x, y);
    }
}
