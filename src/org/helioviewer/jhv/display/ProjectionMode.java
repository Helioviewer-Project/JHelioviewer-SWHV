package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.DisplayView;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, ProjectionScale.ortho) {
        @Override
        public MapContext createMapContext(Camera camera, DisplayView displayView, GridType gridType) {
            return new OrthoMapContext(camera, displayView, gridType);
        }

        @Override
        public Vec3 mouseToSurface(Camera camera, Viewport vp, GridType gridType, int x, int y) {
            return OrthoProjection.mouseToSurface(camera, vp, x, y);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, Viewport vp, GridType gridType, int x, int y) {
            return OrthoProjection.mouseToGrid(camera, vp, gridType, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Camera camera, Viewport vp, GridType gridType, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not support mouseToScreen()");
        }

        @Override
        public Vec2 projectToScreen(Camera camera, Viewport vp, GridType gridType, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
        }

    },
    HPC(GLSLSolarShader.hpc, ProjectionScale.hpc, NonOrthoProjection.Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, ProjectionScale.lati, NonOrthoProjection.Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, ProjectionScale.logpolar, NonOrthoProjection.Kind.POLAR),
    Polar(GLSLSolarShader.polar, ProjectionScale.polar, NonOrthoProjection.Kind.POLAR);

    public final GLSLSolarShader shader;
    public final ProjectionScale scale;
    final NonOrthoProjection.Kind nonOrthoKind;

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale) {
        this(_shader, _scale, null);
    }

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale, NonOrthoProjection.Kind _nonOrthoKind) {
        shader = _shader;
        scale = _scale;
        nonOrthoKind = _nonOrthoKind;
    }

    public MapContext createMapContext(Camera camera, DisplayView displayView, GridType gridType) {
        return new NonOrthoMapContext(camera, displayView, gridType, this);
    }

    public Vec3 mouseToSurface(Camera camera, Viewport vp, GridType gridType, int x, int y) {
        return NonOrthoProjection.mouseToSurface(nonOrthoKind, camera, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToGrid(Camera camera, Viewport vp, GridType gridType, int x, int y) {
        return NonOrthoProjection.mouseToGrid(camera, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToScreen(Camera camera, Viewport vp, GridType gridType, int x, int y) {
        return NonOrthoProjection.mouseToScreen(camera, vp, scale, gridType, x, y);
    }

    public Vec2 projectToScreen(Camera camera, Viewport vp, GridType gridType, Vec3 v) {
        Position viewpoint = camera.getViewpoint();
        return NonOrthoProjection.projectToScreen(nonOrthoKind, viewpoint, scale, gridType.mapRotation(viewpoint), vp, v);
    }
}
