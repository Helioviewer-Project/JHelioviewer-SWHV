package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
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
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;

public class GLListener implements GLEventListener {

    private final ScalableSurface surface;
    private boolean reshaped;

    private static final GLShape blackCircle = new GLShape();

    public GLListener(ScalableSurface _surface) {
        surface = _surface;
    }

    @Override
    public void init(GLAutoDrawable drawable) { // NEDT
        GLContext ctx = drawable.getContext();
        GL2 gl = ctx.getGL().getGL2();
        GLInfo.update(gl);
        GLInfo.updatePixelScale(surface);

        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        if (drawable.getChosenGLCapabilities().getNumSamples() != 0)
            gl.glEnable(GL2.GL_MULTISAMPLE);
        else {
            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        }

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
        GLSLSolarShader.init(gl);
        GLSLLineShader.init(gl);
        GLSLShapeShader.init(gl);

        initBlackCircle(gl);
    }

    private void initBlackCircle(GL2 gl) {
        blackCircle.init(gl);

        double r = 0.996;
        int segments = 360;
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(4 * (segments + 1));
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(4 * (segments + 1));
        for (int n = 0; n <= segments; ++n) {
            double t = -2 * Math.PI * n / segments;
            BufferUtils.put4f(positionBuffer, (float) (/*x +*/ Math.sin(t) * r), (float) (/*y +*/ Math.cos(t) * r), 0, 0);
            colorBuffer.put(BufferUtils.colorBlack);
        }
        positionBuffer.rewind();
        colorBuffer.rewind();
        blackCircle.setData(gl, positionBuffer, colorBuffer);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) { // NEDT
        GLText.dispose();
        GL2 gl = drawable.getGL().getGL2();

        EventQueue.invokeLater(() -> {
            disposeImpl(gl);
            GLInfo.checkGLErrors(gl, "GLListener.dispose()");
        });
    }

    private static void disposeImpl(GL2 gl) {
        Layers.dispose(gl);
        blackCircle.dispose(gl);
        GLSLSolarShader.dispose(gl);
        GLSLLineShader.dispose(gl);
        GLSLShapeShader.dispose(gl);
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
            reshaped = true;
            Displayer.setGLSize(x, y, width, height);
            Displayer.reshapeAll();
            Layers.getMiniviewLayer().reshapeViewport();
            Displayer.render(1);
        });
    }

    public static void renderScene(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                CameraHelper.applyPerspective(camera, vp, gl, blackCircle);
                Layers.render(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawAnnotations(vp, gl);
            }
        }
    }

    public static void renderSceneScale(Camera camera, GL2 gl) {
        if (Displayer.mode == Displayer.DisplayMode.Polar) {
            GridScale.polar.set(0, 360, 0, 0.5 * ImageLayers.getLargestPhysicalSize());
        } else if (Displayer.mode == Displayer.DisplayMode.LogPolar) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, 0.5 * ImageLayers.getLargestPhysicalSize()));
        }

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                CameraHelper.applyPerspectiveLatitudinal(camera, vp, gl);
                Layers.renderScale(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawAnnotations(vp, gl);
            }
        }
    }

    public static void renderFloatScene(Camera camera, GL2 gl) {
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                Layers.renderFloat(camera, vp, gl);
            }
        }
    }

    private static void renderFullFloatScene(Camera camera, GL2 gl) {
        Viewport vp = Displayer.fullViewport;
        gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        Layers.renderFullFloat(camera, vp, gl);
    }

    private static void renderMiniview(GL2 gl) {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Displayer.getMiniCamera();
            miniCamera.timeChanged(Movie.getTime());

            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            CameraHelper.applyPerspective(miniCamera, vp, gl, blackCircle);
            Layers.renderMiniview(miniCamera, vp, gl);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) { // NEDT
        if (!reshaped || !EventQueue.isDispatchThread()) { // seldom
            EventQueue.invokeLater(Displayer::display);
            return;
        }

        GL2 gl = (GL2) drawable.getGL();
        GLInfo.updatePixelScale(surface);

        Layers.prerender(gl);

        Camera camera = Displayer.getCamera();

        if (exporter != null)
            exporter.handleMovieExport(camera, gl);

        if (Displayer.mode == Displayer.DisplayMode.Orthographic) {
            renderScene(camera, gl);
            renderMiniview(gl);
        } else
            renderSceneScale(camera, gl);

        renderFloatScene(camera, gl);
        renderFullFloatScene(camera, gl);

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth(), camera.getViewpoint().distance);
    }

}
