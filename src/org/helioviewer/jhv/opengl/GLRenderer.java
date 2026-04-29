package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.JHVFrame;
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

        JHVFrame.getInteraction().initAnnotations();
    }

    public static void reshape(int glWidth, int glHeight) {
        Display.setGLSize(0, 0, glWidth, glHeight);
        Display.reshapeAll();
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null)
            miniview.reshapeViewport();
    }

    public static void display(boolean whiteBackground) {
        if (whiteBackground)
            GL.glClearColor(1, 1, 1, 0);
        else
            GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);

        Layers.prerender();

        Camera camera = Display.getCamera();

        if (Display.mode.isOrthographic()) {
            renderScene(camera);
            renderMiniview();
        } else
            renderSceneScale(camera);
        renderFullFloatScene(camera);
    }

    public static void dispose() {
        Layers.dispose();
        JHVFrame.getInteraction().disposeAnnotations();
        ExportMovie.dispose();
        GLText.dispose();

        GLSLSolar.quad.dispose();
        GLSLSolarShader.dispose();
        GLSLLineShader.dispose();
        GLSLShapeShader.dispose();
        GLSLTextureShader.dispose();

        JHVGLException.checkErrors("GLRenderer.dispose()");
    }

    public static void remove() {
        dispose();
        Layers.remove();
    }

    static void renderScene(Camera camera) {
        for (Viewport vp : Display.getViewports()) {
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho(vp.aspect);
            GLSLSolarShader.bindScreen(vp);

            GLSLSolarShader.sphere.use();
            GLSLSolar.quad.render();

            Layers.render(camera, vp);
            JHVFrame.getInteraction().drawAnnotations(vp);
            Layers.renderFloat(camera, vp);
        }
    }

    private static void renderMiniview() {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();

            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(vp);

            GL.glDisable(GL.DEPTH_TEST);
            miniview.renderBackground();
            Layers.renderMiniview(miniCamera, vp);
            GL.glEnable(GL.DEPTH_TEST);
        }
    }

    static void renderSceneScale(Camera camera) {
        if (Display.mode.isPolar()) {
            GridScale.polar.set(0, 360, 0, ImageLayerBounds.getLargestRadialSize());
        } else if (Display.mode.isLogPolar()) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayerBounds.getLargestRadialSize()));
        }

        boolean hpcMode = Display.mode.isHpc();
        Region hpcBounds = hpcMode ? getCenteredHpcScaleBounds() : null;
        for (Viewport vp : Display.getViewports()) {
            if (hpcMode) {
                double halfWidth = 0.5 * hpcBounds.width;
                double halfHeight = Math.max(0.5 * hpcBounds.height, halfWidth / vp.aspect);
                halfWidth = halfHeight * vp.aspect;
                GridScale.hpc.set(-halfWidth, halfWidth, -halfHeight, halfHeight);
            }
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(vp);

            Layers.renderScale(camera, vp);
            JHVFrame.getInteraction().drawAnnotations(vp);
            Layers.renderFloat(camera, vp);
        }
    }

    private static void renderFullFloatScene(Camera camera) {
        Viewport vp = Display.fullViewport;
        GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(camera, vp);
    }

    private static Region getCenteredHpcScaleBounds() {
        Region bounds = ImageLayerBounds.getLargestHpcBounds();
        double halfWidth = Math.max(Math.abs(bounds.llx), Math.abs(bounds.urx));
        double halfHeight = Math.max(Math.abs(bounds.lly), Math.abs(bounds.ury));
        if (halfWidth <= 0)
            halfWidth = 5;
        if (halfHeight <= 0)
            halfHeight = 5;
        return new Region(-halfWidth, -halfHeight, 2 * halfWidth, 2 * halfHeight);
    }

}
