package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;

public final class GLRenderer {

    private static MapView mapView = initialMapView();

    private GLRenderer() {}

    private static MapView initialMapView() {
        Camera camera = Display.getCamera();
        return Display.mode.createMapView(camera, Sun.StartEarth, Display.gridType);
    }

    public static Position getDisplayedViewpoint() {
        return mapView.viewpoint();
    }

    public static MapView getMapView() {
        return mapView;
    }

    public static void init() {
        GL.glEnable(GL.BLEND);
        GL.glBlendFunc(GL.ONE, GL.ONE_MINUS_SRC_ALPHA);
        GL.glBlendEquation(GL.FUNC_ADD);

        GL.glEnable(GL.DEPTH_TEST);
        GL.glDepthFunc(GL.LEQUAL);

        GL.glEnable(GL.CULL_FACE);
        GL.glCullFace(GL.BACK);

        GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);

        GLSLSolar.quad.init();
        GLSLSolarShader.init();
        GLSLLineShader.init();
        GLSLShapeShader.init();
        GLSLTextureShader.init();

        Annotations.init();
    }

    public static void reshape(int glWidth, int glHeight) {
        Display.setGLSize(0, 0, glWidth, glHeight);
        Display.reshapeAll();
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null)
            miniview.reshapeViewport();
    }

    public static void display(Position viewpoint) {
        Camera camera = Display.getCamera();
        mapView = Display.mode.createMapView(camera, viewpoint, Display.gridType);

        if (Display.whiteBackground)
            GL.glClearColor(1, 1, 1, 0);
        else
            GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);

        Layers.prerender();

        if (mapView.isOrthographic()) {
            renderScene();
            renderMiniview();
        } else
            renderSceneScale();
        renderFullFloatScene();

        if (ExportMovie.isRecording())
            ExportMovie.handleMovieExport();
    }

    public static void dispose() {
        Layers.dispose();
        Annotations.dispose();
        ExportMovie.dispose();
        GLText.dispose();

        GLSLSolar.quad.dispose();
        GLSLSolarShader.dispose();
        GLSLLineShader.dispose();
        GLSLShapeShader.dispose();
        GLSLTextureShader.dispose();

        JHVGLException.checkErrors("GLRenderer.dispose()");
    }

    static void renderScene() {
        MapView mv = mapView;
        Camera camera = mv.camera();
        MapScale scale = MapScale.ortho;
        for (Viewport vp : Display.getViewports()) {
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho(vp.aspect, mv.cameraWidth(vp), camera.getTranslationX(), camera.getTranslationY(), mv.viewRotation());
            GLSLSolarShader.bindScreen(vp, scale);

            GLSLSolarShader.sphere.use();
            GLSLSolar.quad.render();

            Layers.render(mv, vp, scale);
            Annotations.render(mv, vp, scale);
            Layers.renderFloat(mv, vp, scale);
        }
    }

    private static void renderMiniview() {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();

            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho2D(vp.aspect, miniCamera.getCameraWidth(vp.zoom), miniCamera.getTranslationX(), miniCamera.getTranslationY());
            MapScale scale = MapScale.ortho;
            GLSLSolarShader.bindScreen(vp, scale);

            GL.glDisable(GL.DEPTH_TEST);
            miniview.renderBackground();
            MapView mv = mapView.mode().createMapView(miniCamera, mapView.viewpoint(), miniCamera.getCameraWidth(vp.zoom) / vp.zoom, mapView.gridType());
            Layers.renderMiniview(mv, vp, scale);
            GL.glEnable(GL.DEPTH_TEST);
        }
    }

    static void renderSceneScale() {
        MapView mv = mapView;
        MapMode mode = mv.mode();
        if (mode == MapMode.Polar) {
            MapScale.polar.set(0, 360, 0, ImageLayers.getLargestRadialSize());
        } else if (mode == MapMode.LogPolar) {
            MapScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayers.getLargestRadialSize()));
        }

        boolean hpcMode = mode == MapMode.HPC;
        Region hpcBounds = hpcMode ? ImageLayers.computeHpcScaleBounds() : null;
        Camera camera = mv.camera();
        for (Viewport vp : Display.getViewports()) {
            MapScale scale = mode.scale;
            if (hpcMode) {
                double halfWidth = 0.5 * hpcBounds.width;
                double halfHeight = Math.max(0.5 * hpcBounds.height, halfWidth / vp.aspect);
                halfWidth = halfHeight * vp.aspect;
                MapScale.hpc.set(-halfWidth, halfWidth, -halfHeight, halfHeight);
                scale = MapScale.hpc;
            }
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho2D(vp.aspect, mv.cameraWidth(vp), camera.getTranslationX(), camera.getTranslationY());
            GLSLSolarShader.bindScreen(vp, scale);

            Layers.renderScale(mv, vp, scale);
            Annotations.render(mv, vp, scale);
            Layers.renderFloat(mv, vp, scale);
        }
    }

    private static void renderFullFloatScene() {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(vp);
    }

}
