package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.Movie;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class GLListener implements GLEventListener {

    private final ScalableSurface surface;
    private static final GLSLShape blackCircle = new GLSLShape();

    public static final GLSLSolar glslSolar = new GLSLSolar();

    public GLListener(ScalableSurface _surface) {
        surface = _surface;
    }

    @Override
    public void init(GLAutoDrawable drawable) { // NEDT
        GL2 gl = (GL2) drawable.getGL();
        GLInfo.update(gl);
        GLInfo.updatePixelScale(surface);

        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glEnable(GL2.GL_MULTISAMPLE);

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glEnable(GL2.GL_POINT_SPRITE);

        glslSolar.init(gl);
        GLSLSolarShader.init(gl);
        GLSLLineShader.init(gl);
        GLSLShapeShader.init(gl);
        GLSLTextureShader.init(gl);

        blackCircle.init(gl);
        GLHelper.initCircleFront(gl, blackCircle, 0, 0, 0.9998, 180, BufferUtils.colorBlack);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) { // NEDT
        GL2 gl = (GL2) drawable.getGL();
        EventQueue.invokeLater(() -> {
            disposeImpl(gl);
            GLInfo.checkGLErrors(gl, "GLListener.dispose()");
        });
    }

    private static void disposeImpl(GL2 gl) {
        Layers.dispose(gl);
        blackCircle.dispose(gl);
        GLText.dispose(gl);

        glslSolar.dispose(gl);
        GLSLSolarShader.dispose(gl);
        GLSLLineShader.dispose(gl);
        GLSLShapeShader.dispose(gl);
        GLSLTextureShader.dispose(gl);
    }

    private ExportMovie exporter;

    public void attachExport(ExportMovie me) {
        exporter = me;
    }

    public void detachExport() {
        exporter = null;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { // NEDT
        EventQueue.invokeLater(() -> {
            Display.setGLSize(x, y, width, height);
            Display.reshapeAll();
            Layers.getMiniviewLayer().reshapeViewport();
            Display.render(1);
        });
    }

    public static void renderScene(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                camera.projectionOrtho(vp.aspect, gl, blackCircle);

                Layers.render(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawAnnotations(vp, gl);
                Layers.renderFloat(camera, vp, gl);
            }
        }
    }

    public static void renderSceneScale(Camera camera, GL2 gl) {
        if (Display.mode == Display.DisplayMode.Polar) {
            GridScale.polar.set(0, 360, 0, 0.5 * ImageLayers.getLargestPhysicalSize());
        } else if (Display.mode == Display.DisplayMode.LogPolar) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, 0.5 * ImageLayers.getLargestPhysicalSize()));
        }

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                camera.projectionOrtho2D(vp.aspect);

                Layers.renderScale(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawAnnotations(vp, gl);
                Layers.renderFloat(camera, vp, gl);
            }
        }
    }

    private static void renderFullFloatScene(Camera camera, GL2 gl) {
        Viewport vp = Display.fullViewport;
        gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        // camera.projectionOrtho2D(vp.aspect); not needed currently
        Layers.renderFullFloat(camera, vp, gl);
    }

    private static void renderMiniview(GL2 gl) {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();
            miniCamera.timeChanged(Movie.getTime());

            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp.aspect);

            gl.glDisable(GL2.GL_DEPTH_TEST);
            MiniviewLayer.renderBackground(gl);
            Layers.renderMiniview(miniCamera, vp, gl);
            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) { // NEDT
        if (!EventQueue.isDispatchThread()) { // via reshape(), reject
            return;
        }

        GL2 gl = (GL2) drawable.getGL();
        gl.glFinish();

        GLInfo.updatePixelScale(surface);

        Layers.prerender(gl);

        Camera camera = Display.getCamera();

        if (exporter != null)
            exporter.handleMovieExport(camera, gl);

        if (Display.mode == Display.DisplayMode.Orthographic) {
            renderScene(camera, gl);
            renderMiniview(gl);
        } else
            renderSceneScale(camera, gl);
        renderFullFloatScene(camera, gl);

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth(), camera.getViewpoint().distance);
        // GLInfo.checkGLErrors(gl, "GLListener.display()");
    }

}
