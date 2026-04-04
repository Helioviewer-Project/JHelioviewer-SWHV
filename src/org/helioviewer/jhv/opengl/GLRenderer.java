package org.helioviewer.jhv.opengl;

import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;

final class GLRenderer {

    private GLRenderer() {
    }

    static void init() {
        GL33.glDisable(GL33.GL_TEXTURE_1D);
        GL33.glDisable(GL33.GL_TEXTURE_2D);

        GL33.glEnable(GL33.GL_MULTISAMPLE);

        GL33.glEnable(GL33.GL_BLEND);
        GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA);
        GL33.glBlendEquation(GL33.GL_FUNC_ADD);

        GL33.glEnable(GL33.GL_DEPTH_TEST);
        GL33.glDepthFunc(GL33.GL_LEQUAL);

        GL33.glEnable(GL33.GL_CULL_FACE);
        GL33.glCullFace(GL33.GL_BACK);

        GL33.glClearColor(0, 0, 0, 0);
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);

        GL33.glEnable(GL33.GL_VERTEX_PROGRAM_POINT_SIZE);

        GLSLSolar.quad.init();
        GLSLSolarShader.init();
        GLSLLineShader.init();
        GLSLShapeShader.init();
        GLSLTextureShader.init();

        JHVFrame.getInteraction().initAnnotations();
    }

    static void reshape(int x, int y, int glWidth, int glHeight) {
        Display.setGLSize(x, y, glWidth, glHeight);
        Display.reshapeAll();
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null)
            miniview.reshapeViewport();
    }

    static void display(boolean whiteBackground) {
        if (whiteBackground)
            GL33.glClearColor(1, 1, 1, 0);
        else
            GL33.glClearColor(0, 0, 0, 0);

        Layers.prerender();

        Camera camera = Display.getCamera();

        if (Display.mode.isOrthographic()) {
            renderScene(camera);
            renderMiniview();
        } else
            renderSceneScale(camera);
        renderFullFloatScene(camera);

        Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
        JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance, Display.mode);
    }

    static void dispose() {
        Layers.dispose();
        JHVFrame.getInteraction().disposeAnnotations();
        GLText.dispose();

        GLSLSolar.quad.dispose();
        GLSLSolarShader.dispose();
        GLSLLineShader.dispose();
        GLSLShapeShader.dispose();
        GLSLTextureShader.dispose();

        JHVGLException.checkErrors("GLRenderer.dispose()");
    }

    static void renderScene(Camera camera) {
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            GL33.glViewport(vp.x, vp.yGL, vp.width, vp.height);
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

            GL33.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(vp);

            GL33.glDisable(GL33.GL_DEPTH_TEST);
            miniview.renderBackground();
            Layers.renderMiniview(miniCamera, vp);
            GL33.glEnable(GL33.GL_DEPTH_TEST);
        }
    }

    static void renderSceneScale(Camera camera) {
        if (Display.mode.isPolar()) {
            GridScale.polar.set(0, 360, 0, ImageLayerBounds.getLargestRadialSize());
        } else if (Display.mode.isLogPolar()) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayerBounds.getLargestRadialSize()));
        }

        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);
        boolean hpcMode = Display.mode.isHpc();
        Region hpcBounds = hpcMode ? getCenteredHpcScaleBounds() : null;
        for (Viewport vp : Display.getViewports()) {
            if (hpcMode) {
                double halfWidth = 0.5 * hpcBounds.width;
                double halfHeight = Math.max(0.5 * hpcBounds.height, halfWidth / vp.aspect);
                halfWidth = halfHeight * vp.aspect;
                GridScale.hpc.set(-halfWidth, halfWidth, -halfHeight, halfHeight);
            }
            GL33.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(vp);

            Layers.renderScale(camera, vp);
            JHVFrame.getInteraction().drawAnnotations(vp);
            Layers.renderFloat(camera, vp);
        }
    }

    private static void renderFullFloatScene(Camera camera) {
        Viewport vp = Display.fullViewport;
        GL33.glViewport(vp.x, vp.yGL, vp.width, vp.height);
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
