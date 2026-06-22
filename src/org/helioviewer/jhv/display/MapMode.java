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
    PowerDisk(GLSLSolarShader.diskPower, Kind.DISK);

    enum Kind {
        ORTHOGRAPHIC, HPC, LATITUDINAL, POLAR, DISK
    }

    public final GLSLSolarShader shader;
    final Kind kind;

    public boolean isDisk() {
        return kind == Kind.DISK;
    }

    MapMode(GLSLSolarShader _shader, Kind _kind) {
        shader = _shader;
        kind = _kind;
    }

    public MapView createMapView(Camera camera, Position viewpoint, GridType gridType, MapScale[] scales) {
        return MapView.create(camera, viewpoint, gridType, this, scales);
    }
}
