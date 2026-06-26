package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho, Kind.ORTHOGRAPHIC),
    HPC(GLSLSolarShader.hpc, Kind.HPC),
    Latitudinal(GLSLSolarShader.lati, Kind.LATITUDINAL),
    RadialWarp(GLSLSolarShader.diskPower, Kind.DISK),
    RectWarp(GLSLSolarShader.rectWarp, Kind.POLAR);

    enum Kind {
        ORTHOGRAPHIC, HPC, LATITUDINAL, POLAR, DISK
    }

    public final GLSLSolarShader shader;
    final Kind kind;

    public boolean isDisk() {
        return kind == Kind.DISK;
    }

    // The radial / normalized-fit projections (disk and rectangular unwraps): they live in a
    // normalized radial space and fit the viewport at camera width ~1, independent of the
    // orthographic R_sun FOV.
    public boolean usesFitWidth() {
        return kind == Kind.DISK || kind == Kind.POLAR;
    }

    // The projections whose radial axis follows the Box-Cox p exponent (the shared p slider).
    public boolean usesRadialExponent() {
        return this == RadialWarp || this == RectWarp;
    }

    MapMode(GLSLSolarShader _shader, Kind _kind) {
        shader = _shader;
        kind = _kind;
    }

    public MapView createMapView(Camera camera, Position viewpoint, GridType gridType, MapScale[] scales) {
        return MapView.create(camera, viewpoint, gridType, this, scales);
    }
}
