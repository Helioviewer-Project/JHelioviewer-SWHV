package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.movie.ExportMovie;

public final class GLRenderer {

    private static MapView mapView = initialMapView();

    private static MapView initialMapView() {
        return createMapView(Display.getCamera(), Sun.StartEarth);
    }

    private static MapView createMapView(Camera camera, Position viewpoint) {
        MapMode mode = Display.mode;
        return mode.createMapView(camera, viewpoint, Display.gridType, createScales(mode, Display.getViewports()));
    }

    private static MapScale[] createScales(MapMode mode, Viewport[] viewports) {
        return switch (mode) {
            case Orthographic -> createConstantScales(viewports, MapScale.ortho);
            case HPC -> createHpcScales(viewports);
            case Latitudinal -> createConstantScales(viewports, MapScale.lati);
            case LogPolar -> createConstantScales(viewports, MapScale.logpolar(ImageLayers.getLargestRadialSize()));
            case Polar -> createConstantScales(viewports, MapScale.polar(ImageLayers.getLargestRadialSize()));
            case RadialWarp -> createConstantScales(viewports, MapScale.diskPower(ImageLayers.getLargestRadialSize()));
            case RectWarp -> createConstantScales(viewports, MapScale.diskPower(ImageLayers.getLargestRadialSize()));
        };
    }

    private static MapScale[] createHpcScales(Viewport[] viewports) {
        Region bounds = ImageLayers.computeHpcScaleBounds();
        MapScale[] scales = new MapScale[viewports.length];
        for (Viewport vp : viewports) {
            double halfWidth = 0.5 * bounds.width;
            double halfHeight = Math.max(0.5 * bounds.height, halfWidth / vp.aspect);
            scales[vp.idx] = MapScale.hpc(halfHeight * vp.aspect, halfHeight);
        }
        return scales;
    }

    private static MapScale[] createConstantScales(Viewport[] viewports, MapScale scale) {
        MapScale[] scales = new MapScale[viewports.length];
        for (Viewport vp : viewports)
            scales[vp.idx] = scale;
        return scales;
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
        if (Display.whiteBackground)
            GL.glClearColor(1, 1, 1, 0);
        else
            GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);

        Layers.prerender();

        mapView = createMapView(Display.getCamera(), viewpoint);
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

        GLException.checkErrors("GLRenderer.dispose()");
    }

    static void renderScene() {
        MapView mv = mapView;
        for (Viewport vp : Display.getViewports()) {
            MapScale scale = mv.scale(vp);
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho(vp.aspect, mv.cameraWidth(vp), mv.cameraTranslationX(), mv.cameraTranslationY(), mv.viewRotation());
            GLSLSolarShader.bindScreen(vp, scale);

            GLSLSolarShader.sphere.use();
            GLSLSolar.quad.render();

            Layers.render(mv, vp);
            Annotations.render(mv, vp);
            Layers.renderFloat(mv, vp);
        }
    }

    private static final MapScale[] miniScales = new MapScale[]{MapScale.ortho};

    private static MapView createMiniMapView(Position viewpoint) {
        return MapMode.Orthographic.createMapView(
                Display.getMiniCamera(),
                viewpoint,
                GridType.Viewpoint,
                miniScales
        );
    }

    private static void renderMiniview() {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            MapView mv = createMiniMapView(mapView.viewpoint());

            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho2D(vp.aspect, mv.cameraWidth(vp), mv.cameraTranslationX(), mv.cameraTranslationY());
            MapScale scale = mv.scale(vp);
            GLSLSolarShader.bindScreen(vp, scale);

            GL.glDisable(GL.DEPTH_TEST);
            miniview.renderBackground();
            Layers.renderMiniview(mv, vp);
            GL.glEnable(GL.DEPTH_TEST);
        }
    }

    static void renderSceneScale() {
        MapView mv = mapView;
        for (Viewport vp : Display.getViewports()) {
            MapScale scale = mv.scale(vp);
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho2D(vp.aspect, mv.cameraWidth(vp), mv.cameraTranslationX(), mv.cameraTranslationY());
            GLSLSolarShader.bindScreen(vp, scale);

            Layers.renderScale(mv, vp);
            Annotations.render(mv, vp);
            Layers.renderFloat(mv, vp);
        }
    }

    private static void renderFullFloatScene() {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(vp);
    }

    private GLRenderer() {}
}
