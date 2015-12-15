package org.helioviewer.jhv.gui.components;

import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLShader;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

@SuppressWarnings("serial")
public class MainComponent extends GLCanvas implements GLEventListener {

    public MainComponent() {
        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(getGLProfile()).createDummyAutoDrawable(null, true, getRequestedGLCapabilities(), null);
        sharedDrawable.display();
        // GUI events can lead to context destruction and invalidation of GL objects and state
        setSharedAutoDrawable(sharedDrawable);

        addGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2(); // try to force an exception

        GLInfo.update(gl);
        GLInfo.updatePixelScale(this);

        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        GLSLShader.init(gl);
        ImageViewerGui.getRenderableContainer().init(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        ImageViewerGui.getRenderableContainer().dispose(gl);
        GLSLShader.dispose(gl);
    }

    private ExportMovie exporter;

    public void attachExport(ExportMovie me) {
        exporter = me;
    }

    public void detachExport() {
        exporter = null;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        Displayer.setGLSize(x, y, width, height);
        Displayer.reshapeAll();
        ImageViewerGui.getRenderableMiniview().reshapeViewport();
    }

    private static void renderBlackCircle(GL2 gl, double[] matrix) {
        gl.glPushMatrix();
        gl.glMultMatrixd(matrix, 0);
        {
            gl.glColor3f(0, 0, 0);
            GLHelper.drawCircleFront(gl, 0, 0, 0.98, 30);
        }
        gl.glPopMatrix();
    }

    public static void renderScene(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        Mat4 inverse = camera.getRotation().toMatrix().transpose();
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.y, vp.width, vp.height);
                CameraHelper.applyPerspective(camera, vp, gl);
                renderBlackCircle(gl, inverse.m);
                ImageViewerGui.getRenderableContainer().render(camera, vp, gl);
                ImageViewerGui.getAnnotateInteraction().drawInteractionFeedback(gl);
            }
        }

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth());
    }

    public static void renderSceneLatitudinal(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.y, vp.width, vp.height);
                CameraHelper.applyPerspectiveLatitudinal(camera, vp, gl);
                ImageViewerGui.getRenderableContainer().renderLatitudinal(camera, vp, gl);
            }
        }

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth());
    }

    public static void renderScenePolar(Camera camera, GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.y, vp.width, vp.height);
                CameraHelper.applyPerspectiveLatitudinal(camera, vp, gl);
                ImageViewerGui.getRenderableContainer().renderPolar(camera, vp, gl);
            }
        }

        ImageViewerGui.getZoomStatusPanel().update(camera.getWidth());
    }

    public static void renderFloatScene(Camera camera, GL2 gl) {
        for (Viewport vp : Displayer.getViewports()) {
            if (vp != null) {
                gl.glViewport(vp.x, vp.y, vp.width, vp.height);
                ImageViewerGui.getRenderableContainer().renderFloat(camera, vp, gl);
            }
        }
    }

    public static void renderFullFloatScene(Camera camera, GL2 gl) {
        Viewport vp = Displayer.fullViewport;
        gl.glViewport(vp.x, vp.y, vp.width, vp.height);
        ImageViewerGui.getRenderableContainer().renderFullFloat(camera, vp, gl);
    }

    private static void renderMiniview(GL2 gl) {
        if (ImageViewerGui.getRenderableMiniview().isVisible()) {
            Viewport vp = ImageViewerGui.getRenderableMiniview().getViewport();
            Camera cameraMini = ImageViewerGui.getRenderableMiniview().getCamera();
            cameraMini.timeChanged(Layers.getLastUpdatedTimestamp());

            gl.glViewport(vp.x, vp.y, vp.width, vp.height);
            CameraHelper.applyPerspective(cameraMini, vp, gl);
            ImageViewerGui.getRenderableContainer().renderMiniview(cameraMini, vp, gl);
        }
    }

    public void renderLatitudinal(Camera camera, GL2 gl) {
        renderSceneLatitudinal(camera, gl);
    }

    public void renderPolar(Camera camera, GL2 gl) {
        renderScenePolar(camera, gl);
    }

    public void renderOrtho(Camera camera, GL2 gl) {
        renderScene(camera, gl);
        renderMiniview(gl);
        renderFloatScene(camera, gl);
        renderFullFloatScene(camera, gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        GLInfo.updatePixelScale(this);

        ImageViewerGui.getRenderableContainer().prerender(gl);

        if (exporter != null) {
            exporter.handleMovieExport(gl);
        }

        Camera camera = Displayer.getCamera();

        if (Displayer.mode == Displayer.DisplayMode.POLAR) {
            renderPolar(camera, gl);
        } else if (Displayer.mode == Displayer.DisplayMode.LATITUDINAL) {
            renderLatitudinal(camera, gl);
        } else {
            renderOrtho(camera, gl);
        }

        setRender(camera);
    }

    private static void setRender(Camera camera) {
        if (renderFactor != -1) {
            ImageViewerGui.getRenderableContainer().setRender(camera, renderFactor);
            renderFactor = -1;
        }
    }

    private static double renderFactor = -1;

    public void render(double _renderFactor) {
        renderFactor = _renderFactor;
        repaint();
    }

}
