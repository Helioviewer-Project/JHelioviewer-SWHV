package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, ProjectionScale.ortho) {
        @Override
        public Vec2 projectToScreen(MapContext ctx, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not use projectToScreen()");
        }

        @Override
        public Vec3 mouseToSurface(Camera camera, Viewport vp, GridType gridType, int x, int y) {
            return OrthoProjection.mouseToSurface(camera, vp, x, y);
        }

        @Override
        public Vec2 emitMapVertex(MapContext ctx, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
            OrthoProjection.emitMapVertex(vertex, first, last, radius, color, vexBuf);
            return previous;
        }

        @Override
        public void emitMapPoint(MapContext ctx, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
            OrthoProjection.emitMapPoint(vertex, size, radius, color, vexBuf);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, Viewport vp, GridType gridType, int x, int y) {
            return OrthoProjection.mouseToGrid(camera, vp, gridType, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Camera camera, MapContext ctx, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not use non-ortho mouseToScreen()");
        }

        @Override
        public MapContext createMapContext(Camera camera, Viewport vp, GridType gridType) {
            return new OrthoMapContext(camera, vp, gridType);
        }
    },
    HPC(GLSLSolarShader.hpc, ProjectionScale.hpc, NonOrthoProjection.Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, ProjectionScale.lati, NonOrthoProjection.Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, ProjectionScale.logpolar, NonOrthoProjection.Kind.POLAR),
    Polar(GLSLSolarShader.polar, ProjectionScale.polar, NonOrthoProjection.Kind.POLAR);

    public final GLSLSolarShader shader;
    public final ProjectionScale scale;
    private final NonOrthoProjection.Kind nonOrthoKind;

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale) {
        this(_shader, _scale, null);
    }

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale, NonOrthoProjection.Kind _nonOrthoKind) {
        shader = _shader;
        scale = _scale;
        nonOrthoKind = _nonOrthoKind;
    }

    public MapContext createMapContext(Camera camera, Viewport vp, GridType gridType) {
        return new NonOrthoMapContext(camera, vp, gridType, scale, nonOrthoKind);
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

    public Vec2 projectToScreen(MapContext ctx, Vec3 v) {
        return NonOrthoProjection.projectToScreen(nonOrthoKind, ctx, v);
    }

    public Vec3 mouseToSurface(Camera camera, Viewport vp, GridType gridType, int x, int y) {
        return NonOrthoProjection.mouseToSurface(nonOrthoKind, camera, vp, scale, gridType, x, y);
    }

    public Vec2 emitMapVertex(MapContext ctx, Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        return NonOrthoProjection.emitMapVertex(nonOrthoKind, ctx, vertex, previous, first, last, color, vexBuf);
    }

    public void emitMapPoint(MapContext ctx, Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        NonOrthoProjection.emitMapPoint(nonOrthoKind, ctx, vertex, size, color, vexBuf);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, GridType gridType, int x, int y) {
        return NonOrthoProjection.mouseToGrid(camera, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToScreen(Camera camera, MapContext ctx, int x, int y) {
        return NonOrthoProjection.mouseToScreen(camera, ctx.vp(), ctx.scale(), ctx.gridType(), x, y);
    }
}
