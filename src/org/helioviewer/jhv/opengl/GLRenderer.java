package org.helioviewer.jhv.opengl;

import java.util.Arrays;

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

    private static MapView screenView = createMapView(Display.getCamera(), Sun.StartEarth);

    static MapView createMapView(Camera camera, Position viewpoint) {
        MapMode mode = Display.mode;
        return MapView.create(camera, viewpoint, mode, Display.gridType, createScales(mode, Display.getViewports()));
    }

    private static MapScale[] createScales(MapMode mode, Viewport[] viewports) {
        return switch (mode) {
            case Orthographic -> createConstantScales(viewports, MapScale.ortho);
            case HPC -> createHpcScales(viewports);
            case Latitudinal -> createConstantScales(viewports, MapScale.lati);
            case RadialWarp, RectWarp -> createConstantScales(viewports, MapScale.boxCoxRadial(ImageLayers.getLargestRadialSize(), Display.getWarpLambda()));
        };
    }

    private static MapScale[] createHpcScales(Viewport[] viewports) {
        Region bounds = ImageLayers.computeHpcScaleBounds();
        MapScale[] scales = new MapScale[viewports.length];
        double halfWidth = 0.5 * bounds.width;
        for (Viewport vp : viewports) {
            double halfHeight = Math.max(0.5 * bounds.height, halfWidth / vp.aspect);
            scales[vp.idx] = MapScale.hpc(halfHeight * vp.aspect, halfHeight);
        }
        return scales;
    }

    private static MapScale[] createConstantScales(Viewport[] viewports, MapScale scale) {
        MapScale[] scales = new MapScale[viewports.length];
        Arrays.fill(scales, scale);
        return scales;
    }

    public static Position getDisplayedViewpoint() {
        return screenView.viewpoint();
    }

    public static MapView getMapView() {
        return screenView;
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

        try {
            GLSLScreenShader.init();
            GLSLSphereShader.init();
            GLSLImageShader.init();
            GLSLLineShader.init();
            GLSLMeshShader.init();
            GLSLShapeShader.init();
            GLSLTextureShader.init();
            Annotations.init();
        } catch (RuntimeException | Error e) {
            Annotations.dispose();
            disposeShaders();
            throw e;
        }
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

        screenView = createMapView(Display.getCamera(), viewpoint);
        renderScene(screenView);
        if (screenView.isOrthographic())
            renderMiniview();
        renderFullFloatScene();

        ExportMovie.renderedFrame();
    }

    public static void dispose() {
        Layers.dispose();
        Annotations.dispose();
        ExportMovie.dispose();
        GLText.dispose();

        disposeShaders();
        BufferObject.releaseUploadBuffer();

        GLException.checkErrors("GLRenderer.dispose()");
    }

    private static void disposeShaders() {
        GLSLTextureShader.dispose();
        GLSLShapeShader.dispose();
        GLSLMeshShader.dispose();
        GLSLLineShader.dispose();
        GLSLImageShader.dispose();
        GLSLSphereShader.dispose();
        GLSLScreenShader.dispose();
    }

    static void renderScene(MapView sceneView) {
        boolean orthographic = sceneView.isOrthographic();
        for (Viewport vp : Display.getViewports()) {
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            if (orthographic)
                Transform.ortho(vp.aspect, sceneView.cameraWidth(vp), sceneView.cameraTranslationX(), sceneView.cameraTranslationY(), sceneView.viewRotation());
            else
                Transform.ortho2D(vp.aspect, sceneView.cameraWidth(vp), sceneView.cameraTranslationX(), sceneView.cameraTranslationY());

            GLSLScreenShader.setView(sceneView, vp);

            if (orthographic) {
                GLSLSphereShader.render();
                Layers.render(sceneView, vp);
            } else
                Layers.renderScale(sceneView, vp);
            Annotations.render(sceneView, vp);
            Layers.renderFloat(sceneView, vp);
        }
    }

    private static final MapScale[] MINI_SCALES = {MapScale.ortho};

    private static void renderMiniview() {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview == null || !miniview.isEnabled())
            return;

        Viewport vp = miniview.getViewport();
        MapView miniView = MapView.create(Display.getMiniCamera(), screenView.viewpoint(), MapMode.Orthographic, GridType.Viewpoint, MINI_SCALES);

        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Transform.ortho2D(vp.aspect, miniView.cameraWidth(vp), miniView.cameraTranslationX(), miniView.cameraTranslationY());

        GLSLScreenShader.setView(miniView, vp);

        GL.glDisable(GL.DEPTH_TEST);
        miniview.renderBackground();
        Layers.renderMiniview(miniView, vp);
        GL.glEnable(GL.DEPTH_TEST);
    }

    private static void renderFullFloatScene() {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(vp);
    }

    private GLRenderer() {}
}
