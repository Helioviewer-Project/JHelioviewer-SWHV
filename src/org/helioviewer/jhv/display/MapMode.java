package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho),
    HPC(GLSLSolarShader.hpc),
    Latitudinal(GLSLSolarShader.lati),
    RadialWarp(GLSLSolarShader.radialWarp),
    RectWarp(GLSLSolarShader.rectWarp);

    public final GLSLSolarShader shader;

    public boolean usesNormalizedFitWidth() {
        return this == RadialWarp || this == RectWarp;
    }

    public double normalizedFitWidth() {
        return this == RectWarp ? 1.0 : MapView.NORMALIZED_FIT_WIDTH;
    }

    public boolean usesWarpLambda() {
        return this == RadialWarp || this == RectWarp;
    }

    MapMode(GLSLSolarShader _shader) {
        shader = _shader;
    }

    public MapView createMapView(Camera camera, Position viewpoint, GridType gridType, MapScale[] scales) {
        return MapView.create(camera, viewpoint, gridType, this, scales);
    }
}
