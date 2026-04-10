package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.lang.reflect.InvocationTargetException;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

@SuppressWarnings("serial")
public final class JHVCanvas extends GLCanvas {

    public static final int GLSAMPLES = 4;
    public static String glVersion = "";
    public static int maxTextureSize;
    public static final double[] pixelScale = {1, 1};

    private boolean whiteBack;
    private boolean displayPending;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    public static JHVCanvas create() {
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            GLCapabilities capabilities = setCapabilities(profile);
            JHVCanvas canvas = new JHVCanvas(capabilities);
            canvas.addGLEventListener(createListener(canvas));
            // GUI events can lead to context destruction and invalidation of GL objects and state
            canvas.setSharedAutoDrawable(getSharedDrawable(profile, capabilities));
            return canvas;
        } catch (Exception e) {
            throw glVersionError(e.getMessage() == null ? "Unknown OpenGL error." : e.getMessage());
        }
    }

    private static AssertionError glVersionError(String err) {
        Log.error(err);
        Message.fatalErr("OpenGL fatal error. JHelioviewer is not able to run:\n" + err);
        return new AssertionError(err);
    }

    private static void initGLInfo(GL3 gl) {
        glVersion = "OpenGL " + gl.glGetString(GL3.GL_VERSION);
        Log.info(glVersion);
        if (!gl.isExtensionAvailable("GL_VERSION_3_3"))
            throw glVersionError("OpenGL 3.3 not supported.");

        int[] out = {0};
        gl.glGetIntegerv(GL3.GL_MAX_TEXTURE_SIZE, out, 0);
        maxTextureSize = out[0];
    }

    private void updatePixelScale() {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            pixelScale[0] = 1;
            pixelScale[1] = 1;
            return;
        }

        AffineTransform transform = graphicsConfiguration.getDefaultTransform();
        pixelScale[0] = transform.getScaleX();
        pixelScale[1] = transform.getScaleY();
    }

    private static GLEventListener createListener(JHVCanvas canvas) {
        return new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                canvas.updatePixelScale();
                GL3 gl = (GL3) drawable.getGL();
                initGLInfo(gl);
                GLRenderer.init(gl);
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                canvas.updatePixelScale();
                GLRenderer.reshape(x, y, canvas.glWidth(), canvas.glHeight());
            }

            @Override
            public void display(GLAutoDrawable drawable) {
                GL3 gl = (GL3) drawable.getGL();
                GLRenderer.display(gl, canvas.whiteBack);

                Camera camera = Display.getCamera();
                if (Movie.isRecording())
                    ExportMovie.handleMovieExport(camera, gl);
                Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
                JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance, Display.mode);
                canvas.frameRendered();
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                GLRenderer.dispose((GL3) drawable.getGL());
            }
        };
    }

    private JHVCanvas(GLCapabilitiesImmutable capabilities) {
        super(capabilities);
    }

    public void setWhiteBackground(boolean whiteBackground) {
        whiteBack = whiteBackground;
    }

    private int glWidth() {
        return (int) (getWidth() * pixelScale[0] + .5);
    }

    private int glHeight() {
        return (int) (getHeight() * pixelScale[1] + .5);
    }

    private void frameRendered() {
        fpsCount++;
    }

    public int getFramerate() {
        long now = System.currentTimeMillis();
        long delta = now - fpsTime;

        if (delta > 1000) {
            fps = (int) ((1000L * fpsCount + delta / 2) / delta);
            fpsCount = 0;
            fpsTime = now;
        }
        return fps;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        try {
            super.setBounds(x, y, width, height);
        } catch (RuntimeException | Error e) {
            if (!handleIncompleteResize("setBounds", e))
                throw e;
        }
    }

    @Override
    public void display() {
        if (displayPending)
            return;
        displayPending = true;
        EventQueue.invokeLater(this::displayOnEdt);
    }

    private void displayOnEdt() {
        displayPending = false;
        try {
            super.display();
        } catch (RuntimeException | Error e) {
            if (!handleIncompleteResize("display", e))
                throw e;
        }
    }

    private static GLCapabilities setCapabilities(GLProfile profile) {
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(GLSAMPLES);
        capabilities.setRedBits(8);
        capabilities.setGreenBits(8);
        capabilities.setBlueBits(8);
        capabilities.setAlphaBits(8);
        capabilities.setDepthBits(32);
        return capabilities;
    }

    private static GLAutoDrawable getSharedDrawable(GLProfile profile, GLCapabilities capabilities) {
        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(profile).createDummyAutoDrawable(null, true, capabilities, null);
        sharedDrawable.display();
        return sharedDrawable;
    }

    private static boolean isIncompleteResize(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (current instanceof InvocationTargetException ite)
                current = ite.getTargetException();
            if (!(current instanceof InternalError))
                continue;

            boolean resizeStack = false;
            for (StackTraceElement element : current.getStackTrace()) {
                if ("jogamp.opengl.GLDrawableHelper".equals(element.getClassName()) && "resizeOffscreenDrawable".equals(element.getMethodName())) {
                    resizeStack = true;
                    break;
                }
            }

            String message = current.getMessage();
            if (resizeStack && message != null && message.startsWith("Incomplete resize operation"))
                return true;
        }
        return false;
    }

    private boolean handleIncompleteResize(String operation, Throwable t) {
        if (!isIncompleteResize(t))
            return false;

        Log.warn("Retrying GLCanvas " + operation + " after incomplete resize operation", t);
        EventQueue.invokeLater(() -> {
            revalidate();
            repaint();
            display();
        });
        return true;
    }

}
