package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho, Kind.ORTHOGRAPHIC),
    HPC(GLSLSolarShader.hpc, Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, Kind.LATITUDINAL),
    LogPolar(GLSLSolarShader.logpolar, Kind.POLAR),
    Polar(GLSLSolarShader.polar, Kind.POLAR),
    RadialWarp(GLSLSolarShader.radialWarp, Kind.RADIAL_WARP);

    enum Kind {
        ORTHOGRAPHIC, HPC, LATITUDINAL, POLAR, RADIAL_WARP
    }

    public final GLSLSolarShader shader;
    final Kind kind;

    public boolean usesNormalizedFitWidth() {
        return kind == Kind.RADIAL_WARP;
    }

    public boolean usesWarpLambda() {
        return this == RadialWarp;
    }

    MapMode(GLSLSolarShader _shader, Kind _kind) {
        shader = _shader;
        kind = _kind;
    }

    public MapView createMapView(Camera camera, Position viewpoint, GridType gridType, MapScale[] scales) {
        return MapView.create(camera, viewpoint, gridType, this, scales);
    }
}
