package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.opengl.GLSLSolarShader;

// Orthographic mode renders directly in 3D, while non-orthographic modes project
// through an explicit map basis shared by rendering and mouse unprojection.
public enum MapMode {
    Orthographic(GLSLSolarShader.ortho, MapScale.ortho) {
        @Override
        public MapView createMapView(Camera camera, RenderView renderView, GridType gridType) {
            return MapView.orthographic(camera, renderView, gridType);
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
        return MapView.projected(camera, renderView, gridType, this);
    }
}
