package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho, MapScale.ortho) {
        @Override
        public MapView createMapView(Camera camera, RenderView renderView, GridType gridType) {
            return new OrthographicView(camera, renderView, gridType);
        }

        @Override
        public Vec3 mouseToSurface(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
            return OrthographicMap.mouseToSurface(camera, renderView, vp, x, y);
        }

        @Override
        public Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
            return OrthographicMap.mouseToGrid(camera, renderView, vp, gridType, x, y);
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
    HPC(GLSLSolarShader.hpc, MapScale.hpc, ProjectedMap.Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, MapScale.lati, ProjectedMap.Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, MapScale.logpolar, ProjectedMap.Kind.POLAR),
    Polar(GLSLSolarShader.polar, MapScale.polar, ProjectedMap.Kind.POLAR);

    public final GLSLSolarShader shader;
    public final MapScale scale;
    final ProjectedMap.Kind projectedKind;

    MapMode(GLSLSolarShader _shader, MapScale _scale) {
        this(_shader, _scale, null);
    }

    MapMode(GLSLSolarShader _shader, MapScale _scale, ProjectedMap.Kind _projectedKind) {
        shader = _shader;
        scale = _scale;
        projectedKind = _projectedKind;
    }

    public MapView createMapView(Camera camera, RenderView renderView, GridType gridType) {
        return new ProjectedView(camera, renderView, gridType, this);
    }

    public Vec3 mouseToSurface(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
        return ProjectedMap.mouseToSurface(projectedKind, camera, renderView, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToGrid(Camera camera, RenderView renderView, Viewport vp, GridType gridType, int x, int y) {
        return ProjectedMap.mouseToGrid(camera, renderView, vp, scale, gridType, x, y);
    }

    public Vec2 mouseToScreen(Camera camera, RenderView renderView, Viewport vp, int x, int y) {
        return ProjectedMap.mouseToScreen(camera, renderView, vp, scale, x, y);
    }

    public Vec2 projectToScreen(RenderView renderView, Viewport vp, GridType gridType, Vec3 v) {
        Position viewpoint = renderView.viewpoint();
        return ProjectedMap.projectToScreen(projectedKind, viewpoint, scale, gridType.mapRotation(viewpoint), vp, v);
    }
}
