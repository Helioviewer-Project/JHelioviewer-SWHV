package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum ProjectionMode {
    Orthographic(GLSLSolarShader.ortho, ProjectionScale.ortho) {
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
}
