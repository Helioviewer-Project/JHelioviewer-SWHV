package org.helioviewer.jhv.gui.components;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLShader;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.awt.GLCanvas;

@SuppressWarnings("serial")
public class MainComponent extends GLCanvas implements GLEventListener {

    public MainComponent() {
        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(getGLProfile()).createDummyAutoDrawable(null, true, getRequestedGLCapabilities(), null);
        sharedDrawable.display();

        // GUI events can lead to context destruction and invalidation of GL objects and state
        setSharedAutoDrawable(sharedDrawable);

        addGLEventListener(this);
        Displayer.setDisplayComponent(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) throws GLException {
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
        Displayer.setGLSize(width, height);
        Displayer.reshapeAll();
    }

    public static void renderScene(GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        for (GL3DViewport vp : Displayer.getViewports()) {
            if (vp.isVisible() && vp.isActive()) {
                GL3DCamera camera = vp.getCamera();
                camera.updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
                gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth(), vp.getHeight());
                camera.applyPerspective(gl);
                ImageViewerGui.getRenderableContainer().render(gl, vp);
                camera.getAnnotateInteraction().drawInteractionFeedback(gl);
            }
        }
    }

    public static void renderFloatScene(GL2 gl) {
        for (GL3DViewport vp : Displayer.getViewports()) {
            if (vp.isVisible() && vp.isActive()) {
                GL3DCamera camera = vp.getCamera();
                camera.updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
                gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth(), vp.getHeight());
                ImageViewerGui.getRenderableContainer().renderFloat(gl, vp);
            }
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        GLInfo.updatePixelScale(this);

        ImageViewerGui.getRenderableContainer().prerender(gl);

        if (exporter != null) {
            exporter.handleMovieExport(gl);
        }

        renderScene(gl);

        GL3DViewport vp = ImageViewerGui.getRenderableMiniview().getViewport();
        if (vp.isVisible()) {
            GL3DCamera camera = vp.getCamera();
            camera.updateRotation(Layers.getLastUpdatedTimestamp(), null);
            camera.updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
            gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth(), vp.getHeight());
            camera.applyPerspective(gl);
            ImageViewerGui.getRenderableContainer().renderMiniview(gl, vp);
        }
        renderFloatScene(gl);
    }

}
