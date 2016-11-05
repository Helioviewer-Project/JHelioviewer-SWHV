package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;

public class GLListener implements GLEventListener {

    private final ScalableSurface surface;

    public GLListener(ScalableSurface surface) {
        this.surface = surface;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GLContext ctx = drawable.getContext();
        GL2 gl = ctx.getGL().getGL2();
        boolean multi = drawable.getChosenGLCapabilities().getNumSamples() != 0;

        EventQueue.invokeLater(() -> {
            ctx.makeCurrent();
            initImpl(gl, multi);
            ctx.release();
        });
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GLText.dispose();
        final GL2 gl = drawable.getGL().getGL2();

        EventQueue.invokeLater(() -> {
            disposeImpl(gl);
            GLInfo.checkGLErrors(gl, "GLListener.dispose()");
        });
    }

    private void initImpl(GL2 gl, boolean multi) {
        GLInfo.update(gl);
        GLInfo.updatePixelScale(surface);

        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        if (multi)
            gl.glEnable(GL2.GL_MULTISAMPLE);
        else {
            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        }

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        GLSLSolarShader.init(gl);
        ImageViewerGui.getRenderableContainer().init(gl);
    }

    private void disposeImpl(GL2 gl) {
        ImageViewerGui.getRenderableContainer().dispose(gl);
        GLSLSolarShader.dispose(gl);
    }

    private ExportMovie exporter;

    public void attachExport(ExportMovie me) {
        exporter = me;
    }

    public void detachExport() {
        exporter = null;
    }

    public boolean isRecording() {
        return exporter != null;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        EventQueue.invokeLater(() -> {
            Displayer.setGLSize(x, y, width, height);
            Displayer.reshapeAll();
            ImageViewerGui.getRenderableMiniview().reshapeViewport();
            Displayer.render(1);
        });
    }

    private static void renderBlackCircle(GL2 gl, double[] matrix) {
        gl.glPushMatrix();
        gl.glMultMatrixd(matrix, 0);
        {
            gl.glColor3f(0, 0, 0);
            GLHelper.drawCircleFront(gl, 0, 0, 0.98 * Sun.Radius, 30);
        }
        gl.glPopMatrix();
    }

    public static void renderScene(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        Mat4 inverse = camera.getRotation().toMatrix().transpose();
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                CameraHelper.applyPerspective(camera, vp, gl);
                renderBlackCircle(gl, inverse.m);
                ImageViewerGui.getRenderableContainer().render(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawInteractionFeedback(vp, gl);
            }
        }
    }

    public static void renderSceneScale(Camera camera, GL2 gl) {
        if (Displayer.mode == Displayer.DisplayMode.POLAR) {
            GridScale.polar.set(0, 360, 0, 0.5 * Layers.getLargestPhysicalSize());
        } else if (Displayer.mode == Displayer.DisplayMode.LOGPOLAR) {
            GridScale.logpolar.set(0, 360, 0.05, 0.5 * Layers.getLargestPhysicalSize());
        }

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                CameraHelper.applyPerspectiveLatitudinal(camera, vp, gl);
                gl.glPushMatrix();
                gl.glTranslatef((float) (camera.getCurrentTranslation().x), (float) (camera.getCurrentTranslation().y), 0f);
                ImageViewerGui.getRenderableContainer().renderScale(camera, vp, gl, Displayer.mode.getSolarShader(), GridScale.current);
                ImageViewerGui.getAnnotateInteraction().drawInteractionFeedback(vp, gl);
                gl.glPopMatrix();
            }
        }
    }

    public static void renderFloatScene(Camera camera, GL2 gl) {
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
                ImageViewerGui.getRenderableContainer().renderFloat(camera, vp, gl);
            }
        }
    }

    private static void renderFullFloatScene(Camera camera, GL2 gl) {
        Viewport vp = Displayer.fullViewport;
        gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        ImageViewerGui.getRenderableContainer().renderFullFloat(camera, vp, gl);
    }

    private static void renderMiniview(GL2 gl) {
        if (ImageViewerGui.getRenderableMiniview().isVisible()) {
            Viewport vp = ImageViewerGui.getRenderableMiniview().getViewport();
            Camera cameraMini = ImageViewerGui.getRenderableMiniview().getCamera();
            cameraMini.timeChanged(Layers.getLastUpdatedTimestamp());

            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            CameraHelper.applyPerspective(cameraMini, vp, gl);
            ImageViewerGui.getRenderableContainer().renderMiniview(cameraMini, vp, gl);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(Displayer::display);
        }

        GL2 gl = (GL2) drawable.getGL();
        GLInfo.updatePixelScale(surface);

        ImageViewerGui.getRenderableContainer().prerender(gl);

        Camera camera = Displayer.getCamera();

        if (exporter != null) {
            exporter.handleMovieExport(camera, gl);
        }

        if (Displayer.mode == Displayer.DisplayMode.ORTHO) {
            renderScene(camera, gl);
            renderMiniview(gl);
        } else {
            renderSceneScale(camera, gl);
        }

        renderFloatScene(camera, gl);
        renderFullFloatScene(camera, gl);

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth(), camera.getViewpoint().distance);
    }

}
