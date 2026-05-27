package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho, MapScale.ortho) {
        @Override
        public MapView createMapView(Camera camera, Position viewpoint, GridType gridType) {
            return MapView.orthographic(camera, viewpoint, gridType);
        }

        @Override
        public MapView createMapView(Camera camera, Position viewpoint, double width, GridType gridType) {
            return MapView.orthographic(camera, viewpoint, width, gridType);
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

    public MapView createMapView(Camera camera, Position viewpoint, GridType gridType) {
        return MapView.projected(camera, viewpoint, gridType, this);
    }

    public MapView createMapView(Camera camera, Position viewpoint, double width, GridType gridType) {
        return MapView.projected(camera, viewpoint, width, gridType, this);
    }
}
