package org.helioviewer.jhv.opengl;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL20.GL_VERTEX_PROGRAM_POINT_SIZE;
import static org.lwjgl.opengl.GL30.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_FUNC_ADD;

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
import org.helioviewer.jhv.layers.Movie;

import com.jogamp.opengl.GL3;

final class GLRenderer {

    private GLRenderer() {
    }

    static void init(GL3 gl) {
        glDisable(GL_TEXTURE_1D);
        glDisable(GL_TEXTURE_2D);

        glEnable(GL_MULTISAMPLE);

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glBlendEquation(GL_FUNC_ADD);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);

        GLSLSolar.quad.init(gl);
        GLSLSolarShader.init(gl);
        GLSLLineShader.init(gl);
        GLSLShapeShader.init(gl);
        GLSLTextureShader.init(gl);

        JHVFrame.getInteraction().initAnnotations(gl);
    }

    static void reshape(int x, int y, int glWidth, int glHeight) {
        Display.setGLSize(x, y, glWidth, glHeight);
        Display.reshapeAll();
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null)
            miniview.reshapeViewport();
    }

    static void display(GL3 gl, boolean whiteBackground) {
        if (whiteBackground)
            glClearColor(1, 1, 1, 0);
        else
            glClearColor(0, 0, 0, 0);

        Layers.prerender(gl);

        Camera camera = Display.getCamera();

        if (Movie.isRecording())
            ExportMovie.handleMovieExport(camera, gl);

        if (Display.mode.isOrthographic()) {
            renderScene(camera, gl);
            renderMiniview(gl);
        } else
            renderSceneScale(camera, gl);
        renderFullFloatScene(camera, gl);

        Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
        JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance, Display.mode);
    }

    static void dispose(GL3 gl) {
        Layers.dispose(gl);
        JHVFrame.getInteraction().disposeAnnotations(gl);
        GLText.dispose(gl);

        GLSLSolar.quad.dispose(gl);
        GLSLSolarShader.dispose(gl);
        GLSLLineShader.dispose(gl);
        GLSLShapeShader.dispose(gl);
        GLSLTextureShader.dispose(gl);

        JHVGLException.checkErrors(gl, "GLRenderer.dispose()");
    }

    static void renderScene(Camera camera, GL3 gl) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho(vp.aspect);
            GLSLSolarShader.bindScreen(gl, vp);

            GLSLSolarShader.sphere.use(gl);
            GLSLSolar.quad.render(gl);

            Layers.render(camera, vp, gl);
            JHVFrame.getInteraction().drawAnnotations(vp, gl);
            Layers.renderFloat(camera, vp, gl);
        }
    }

    private static void renderMiniview(GL3 gl) {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();

            glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(gl, vp);

            glDisable(GL_DEPTH_TEST);
            miniview.renderBackground(gl);
            Layers.renderMiniview(miniCamera, vp, gl);
            glEnable(GL_DEPTH_TEST);
        }
    }

    static void renderSceneScale(Camera camera, GL3 gl) {
        if (Display.mode.isPolar()) {
            GridScale.polar.set(0, 360, 0, ImageLayerBounds.getLargestRadialSize());
        } else if (Display.mode.isLogPolar()) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, ImageLayerBounds.getLargestRadialSize()));
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        boolean hpcMode = Display.mode.isHpc();
        Region hpcBounds = hpcMode ? getCenteredHpcScaleBounds() : null;
        for (Viewport vp : Display.getViewports()) {
            if (hpcMode) {
                double halfWidth = 0.5 * hpcBounds.width;
                double halfHeight = Math.max(0.5 * hpcBounds.height, halfWidth / vp.aspect);
                halfWidth = halfHeight * vp.aspect;
                GridScale.hpc.set(-halfWidth, halfWidth, -halfHeight, halfHeight);
            }
            glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho2D(vp.aspect);
            GLSLSolarShader.bindScreen(gl, vp);

            Layers.renderScale(camera, vp, gl);
            JHVFrame.getInteraction().drawAnnotations(vp, gl);
            Layers.renderFloat(camera, vp, gl);
        }
    }

    private static void renderFullFloatScene(Camera camera, GL3 gl) {
        Viewport vp = Display.fullViewport;
        glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(camera, vp, gl);
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
