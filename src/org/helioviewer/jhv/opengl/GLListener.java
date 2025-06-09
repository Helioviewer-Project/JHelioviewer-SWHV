package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.Movie;
//import org.helioviewer.jhv.layers.MovieDisplay;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

public class GLListener implements GLEventListener {

    private final GLCanvas canvas;
    private boolean whiteBack;

    public static final GLSLSolar glslSolar = new GLSLSolar();

    public GLListener(GLCanvas _canvas) {
        canvas = _canvas;
    }

    public void setWhiteBack(boolean b) {
        whiteBack = b;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL3 gl = (GL3) drawable.getGL();
        GLInfo.get(gl);
        GLInfo.updatePixelScale(canvas);

        gl.glDisable(GL3.GL_TEXTURE_1D);
        gl.glDisable(GL3.GL_TEXTURE_2D);

        gl.glEnable(GL3.GL_MULTISAMPLE);

        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_ONE, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);

        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LEQUAL);

        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE);

        glslSolar.init(gl);
        GLSLSolarShader.init(gl);
        GLSLLineShader.init(gl);
        GLSLShapeShader.init(gl);
        GLSLTextureShader.init(gl);

        JHVFrame.getInteraction().initAnnotations(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL3 gl = (GL3) drawable.getGL();
        Layers.dispose(gl);
        JHVFrame.getInteraction().disposeAnnotations(gl);
        GLText.dispose(gl);

        glslSolar.dispose(gl);
        GLSLSolarShader.dispose(gl);
        GLSLLineShader.dispose(gl);
        GLSLShapeShader.dispose(gl);
        GLSLTextureShader.dispose(gl);

        GLInfo.checkGLErrors(gl, "GLListener.dispose()");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        Display.setGLSize(x, y, (int) (canvas.getWidth() * GLInfo.pixelScale[0] + .5), (int) (canvas.getHeight() * GLInfo.pixelScale[1] + .5));
        Display.reshapeAll();
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null)
            miniview.reshapeViewport();
        // MovieDisplay.render(1);
    }

    public static void renderScene(Camera camera, GL3 gl) {
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho(vp.aspect);

            GLSLSolarShader.sphere.use(gl);
            GLSLSolarShader.sphere.bindInverseCamera(gl);
            GLSLSolarShader.sphere.bindViewport(gl, vp.x, vp.yGL, vp.width, vp.height);
            glslSolar.render(gl);

            Layers.render(camera, vp, gl);
            JHVFrame.getInteraction().drawAnnotations(vp, gl);
            Layers.renderFloat(camera, vp, gl);
        }
    }

    public static void renderSceneScale(Camera camera, GL3 gl) {
        if (Display.mode == Display.ProjectionMode.Polar) {
            GridScale.polar.set(0, 360, 0, 0.5 * ImageLayers.getLargestPhysicalSize());
        } else if (Display.mode == Display.ProjectionMode.LogPolar) {
            GridScale.logpolar.set(0, 360, 0.05, Math.max(0.05, 0.5 * ImageLayers.getLargestPhysicalSize()));
        }

        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
        for (Viewport vp : Display.getViewports()) {
            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            camera.projectionOrtho2D(vp.aspect);

            Layers.renderScale(camera, vp, gl);
            JHVFrame.getInteraction().drawAnnotations(vp, gl);
            Layers.renderFloat(camera, vp, gl);
        }
    }

    private static void renderFullFloatScene(Camera camera, GL3 gl) {
        Viewport vp = Display.fullViewport;
        gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
        // camera.projectionOrtho2D(vp.aspect); not needed currently
        Layers.renderFullFloat(camera, vp, gl);
    }

    private static void renderMiniview(GL3 gl) {
        MiniviewLayer miniview = Layers.getMiniviewLayer();
        if (miniview != null && miniview.isEnabled()) {
            Viewport vp = miniview.getViewport();
            Camera miniCamera = Display.getMiniCamera();
            // miniCamera.timeChanged(Movie.getTime());

            gl.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            miniCamera.projectionOrtho2D(vp.aspect);

            gl.glDisable(GL3.GL_DEPTH_TEST);
            miniview.renderBackground(gl);
            Layers.renderMiniview(miniCamera, vp, gl);
            gl.glEnable(GL3.GL_DEPTH_TEST);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GLInfo.updatePixelScale(canvas);
        GL3 gl = (GL3) drawable.getGL();
        gl.glFinish();

        if (whiteBack)
            gl.glClearColor(1, 1, 1, 0);
        else
            gl.glClearColor(0, 0, 0, 0);

        Layers.prerender(gl);

        Camera camera = Display.getCamera();

        if (Movie.isRecording())
            ExportMovie.handleMovieExport(camera, gl);

        if (Display.mode == Display.ProjectionMode.Orthographic) {
            renderScene(camera, gl);
            renderMiniview(gl);
        } else
            renderSceneScale(camera, gl);
        renderFullFloatScene(camera, gl);

        fpsCount++;
        Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
        JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance);
        // GLInfo.checkGLErrors(gl, "GLListener.display()");
    }

    private static int fps;
    private static int fpsCount;
    private static long fpsTime = System.currentTimeMillis();

    public static int getFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - fpsTime;

        if (delta > 1000) {
            fps = (int) (1000 * fpsCount / (double) delta + .5);
            fpsCount = 0;
            fpsTime = currentTime;
        }
        return fps;
    }

}
