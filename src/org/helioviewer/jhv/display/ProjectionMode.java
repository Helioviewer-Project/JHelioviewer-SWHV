package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, ProjectionScale.ortho) {
        @Override
        public MapContext createMapContext(Camera camera, RenderView renderView, GridType gridType) {
            return new OrthographicMapContext(camera, renderView, gridType);
        }

        @Override
        public Vec3 mouseToSurface(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
            return OrthographicProjection.mouseToSurface(camera, renderView, vp, x, y);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
            return OrthographicProjection.mouseToGrid(camera, renderView, vp, gridType, x, y);
        }

        @Override
        public Vec2 mouseToScreen(Camera camera, RenderView renderView, Viewport vp, int x, int y) {
            throw new UnsupportedOperationException("Orthographic mode does not support mouseToScreen()");
        }

        @Override
        public Vec2 projectToScreen(RenderView renderView, Viewport vp, GridType gridType, Vec3 v) {
            throw new UnsupportedOperationException("Orthographic mode does not support projectToScreen()");
        }

    },
    HPC(GLSLSolarShader.hpc, ProjectionScale.hpc, ProjectedMapProjection.Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, ProjectionScale.lati, ProjectedMapProjection.Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, ProjectionScale.logpolar, ProjectedMapProjection.Kind.POLAR),
    Polar(GLSLSolarShader.polar, ProjectionScale.polar, ProjectedMapProjection.Kind.POLAR);

    public final GLSLSolarShader shader;
    public final ProjectionScale scale;
    final ProjectedMapProjection.Kind projectedKind;

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale) {
        this(_shader, _scale, null);
    }

    ProjectionMode(GLSLSolarShader _shader, ProjectionScale _scale, ProjectedMapProjection.Kind _projectedKind) {
        shader = _shader;
        scale = _scale;
        projectedKind = _projectedKind;
    }

    public MapContext createMapContext(Camera camera, RenderView renderView, GridType gridType) {
        return new ProjectedMapContext(camera, renderView, gridType, this);
    }

    public Vec3 mouseToSurface(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
        return ProjectedMapProjection.mouseToSurface(projectedKind, camera, renderView, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
        return ProjectedMapProjection.mouseToGrid(camera, renderView, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToScreen(Camera camera, RenderView renderView, Viewport vp, int x, int y) {
        return ProjectedMapProjection.mouseToScreen(camera, renderView, vp, scale, x, y);
    }

    public Vec2 projectToScreen(RenderView renderView, Viewport vp, GridType gridType, Vec3 v) {
        Position viewpoint = renderView.viewpoint();
        return ProjectedMapProjection.projectToScreen(projectedKind, viewpoint, scale, gridType.mapRotation(viewpoint), vp, v);
    }
}
