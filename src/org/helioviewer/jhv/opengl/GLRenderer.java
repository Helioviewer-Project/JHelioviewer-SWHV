package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Annotations;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;

public final class GLRenderer {

    private GLRenderer() {}

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

    public static void display() {
        if (Display.whiteBackground)
            GL.glClearColor(1, 1, 1, 0);
        else
            GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);

        Layers.prerender();

        if (Display.mode == ProjectionMode.Orthographic) {
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
        Camera camera = Display.getCamera();
        MapContext ctx = Display.getMapContext();
        ProjectionScale scale = ProjectionScale.ortho;
        for (Viewport vp : Display.getViewports()) {
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho(vp);
            GLSLSolarShader.bindScreen(vp, scale);

            GLSLSolarShader.sphere.use();
            GLSLSolar.quad.render();

            Layers.render(ctx, vp, scale);
            Annotations.render(ctx, vp, scale);
            Layers.renderFloat(ctx, vp, scale);
        }
    }

    private static void renderMiniview() {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();

            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp);
            ProjectionScale scale = ProjectionScale.ortho;
            GLSLSolarShader.bindScreen(vp, scale);

            GL.glDisable(GL.DEPTH_TEST);
            miniview.renderBackground();
            Layers.renderMiniview(Display.getMiniMapContext(), vp, scale);
            GL.glEnable(GL.DEPTH_TEST);
        }
    }

    static void renderSceneScale() {
        if (Display.mode == ProjectionMode.Polar) {
            ProjectionScale.polar.set(0, 360, 0, ImageLayerBounds.getLargestRadialSize());
        } else if (Display.mode == ProjectionMode.LogPolar) {
            ProjectionScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayerBounds.getLargestRadialSize()));
        }

        boolean hpcMode = Display.mode == ProjectionMode.HPC;
        Region hpcBounds = hpcMode ? ImageLayerBounds.getCenteredHpcScaleBounds() : null;
        Camera camera = Display.getCamera();
        MapContext ctx = Display.getMapContext();
        for (Viewport vp : Display.getViewports()) {
            ProjectionScale scale = Display.mode.scale;
            if (hpcMode) {
                double halfWidth = 0.5 * hpcBounds.width;
                double halfHeight = Math.max(0.5 * hpcBounds.height, halfWidth / vp.aspect);
                halfWidth = halfHeight * vp.aspect;
                ProjectionScale.hpc.set(-halfWidth, halfWidth, -halfHeight, halfHeight);
                scale = ProjectionScale.hpc;
            }
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho2D(vp);
            GLSLSolarShader.bindScreen(vp, scale);

            Layers.renderScale(ctx, vp, scale);
            Annotations.render(ctx, vp, scale);
            Layers.renderFloat(ctx, vp, scale);
        }
    }

    private static void renderFullFloatScene() {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(vp);
    }

}
