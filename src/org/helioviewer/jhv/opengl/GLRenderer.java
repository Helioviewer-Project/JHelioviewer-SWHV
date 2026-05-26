package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.RenderView;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.wcs.ImageBounds;

public final class GLRenderer {

    private static RenderView renderView = Display.getCamera().renderView(Sun.StartEarth);

    private GLRenderer() {}

    public static Position getDisplayedViewpoint() {
        return renderView.viewpoint();
    }

    public static RenderView getRenderView() {
        return renderView;
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
        renderView = Display.getCamera().renderView(viewpoint);

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
        MapContext ctx = Display.mode.createMapContext(camera, renderView, Display.gridType);
        ProjectionScale scale = ProjectionScale.ortho;
        for (Viewport vp : Display.getViewports()) {
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            Transform.ortho(vp.aspect, renderView.cameraWidth(vp.zoom), camera.getTranslationX(), camera.getTranslationY(), renderView.viewRotation());
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
            Transform.ortho2D(vp.aspect, miniCamera.getCameraWidth(vp.zoom), miniCamera.getTranslationX(), miniCamera.getTranslationY());
            ProjectionScale scale = ProjectionScale.ortho;
            GLSLSolarShader.bindScreen(vp, scale);

            GL.glDisable(GL.DEPTH_TEST);
            miniview.renderBackground();
            RenderView miniView = miniCamera.view(renderView.viewpoint(), miniCamera.getCameraWidth(vp.zoom) / vp.zoom);
            MapContext ctx = Display.mode.createMapContext(miniCamera, miniView, Display.gridType);
            Layers.renderMiniview(ctx, vp, scale);
            GL.glEnable(GL.DEPTH_TEST);
        }
    }

    static void renderSceneScale() {
        if (Display.mode == ProjectionMode.Polar) {
            ProjectionScale.polar.set(0, 360, 0, ImageLayers.getLargestRadialSize());
        } else if (Display.mode == ProjectionMode.LogPolar) {
            ProjectionScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayers.getLargestRadialSize()));
        }

        boolean hpcMode = Display.mode == ProjectionMode.HPC;
        Region hpcBounds = hpcMode ? computeHpcSceneScaleBounds() : null;
        Camera camera = Display.getCamera();
        MapContext ctx = Display.mode.createMapContext(camera, renderView, Display.gridType);
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
            Transform.ortho2D(vp.aspect, renderView.cameraWidth(vp.zoom), camera.getTranslationX(), camera.getTranslationY());
            GLSLSolarShader.bindScreen(vp, scale);

            Layers.renderScale(ctx, vp, scale);
            Annotations.render(ctx, vp, scale);
            Layers.renderFloat(ctx, vp, scale);
        }
    }

    public static Region computeHpcSceneScaleBounds() {
        Region bounds = getHpcSceneImageBounds();
        double halfWidth = Math.max(Math.abs(bounds.llx), Math.abs(bounds.urx));
        double halfHeight = Math.max(Math.abs(bounds.lly), Math.abs(bounds.ury));
        if (halfWidth <= 0)
            halfWidth = 5;
        if (halfHeight <= 0)
            halfHeight = 5;
        return new Region(-halfWidth, -halfHeight, 2 * halfWidth, 2 * halfHeight);
    }

    private static Region getHpcSceneImageBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            Region bounds = ImageBounds.hpc(layer.getMetaData());
            minX = Math.min(minX, bounds.llx);
            maxX = Math.max(maxX, bounds.urx);
            minY = Math.min(minY, bounds.lly);
            maxY = Math.max(maxY, bounds.ury);
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minY) || !Double.isFinite(maxY))
            return new Region(-5, -5, 10, 10);
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    private static void renderFullFloatScene() {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(vp);
    }

}
