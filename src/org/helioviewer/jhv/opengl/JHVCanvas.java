package org.helioviewer.jhv.opengl;

import static org.lwjgl.opengl.GL.createCapabilities;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

@SuppressWarnings("serial")
public final class JHVCanvas extends AWTGLCanvas {

    public static final int GLSAMPLES = 4;
    public static String glVersion = "";
    public static int maxTextureSize;
    public static final double[] pixelScale = {1, 1};

    private boolean whiteBack;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    private GLContext externalContext;
    private boolean rendererInitialized;
    private boolean externalContextInitDeferred;
    private int lastGlWidth = -1;
    private int lastGlHeight = -1;

    public static JHVCanvas create() {
        try {
            return new JHVCanvas(createData());
        } catch (Exception e) {
            throw glVersionError(e.getMessage() == null ? "Unknown OpenGL error." : e.getMessage());
        }
    }

    private static GLData createData() {
        GLData data = new GLData();
        data.samples = GLSAMPLES;
        data.redSize = 8;
        data.greenSize = 8;
        data.blueSize = 8;
        data.alphaSize = 8;
        data.depthSize = 32;
        data.majorVersion = 3;
        data.minorVersion = 3;
        data.profile = GLData.Profile.CORE;
        return data;
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

    private JHVCanvas(GLData data) {
        super(data);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lastGlWidth = -1;
                lastGlHeight = -1;
                EventQueue.invokeLater(JHVCanvas.this::display);
            }
        });
    }

    @Override
    public void initGL() {
        updatePixelScale();
        createCapabilities();
        ensureRendererInitialized();
    }

    @Override
    public void paintGL() {
        updatePixelScale();
        ensureRendererInitialized();
        if (!rendererInitialized)
            return;

        GLContext currentContext = externalContext;
        if (currentContext == null)
            return;

        try {
            currentContext.makeCurrent();
            GL3 gl = currentContext.getGL().getGL3();
            int glWidth = glWidth();
            int glHeight = glHeight();
            if (glWidth != lastGlWidth || glHeight != lastGlHeight) {
                GLRenderer.reshape(0, 0, glWidth, glHeight);
                lastGlWidth = glWidth;
                lastGlHeight = glHeight;
            }
            GLRenderer.display(gl, whiteBack);
            swapBuffers();
            frameRendered();
        } finally {
            currentContext.release();
        }
    }

    public void display() {
        render();
    }

    @Override
    public void removeNotify() {
        disposeRenderer();
        super.removeNotify();
    }

    public void setWhiteBackground(boolean whiteBackground) {
        whiteBack = whiteBackground;
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

    private int glWidth() {
        return (int) (getWidth() * pixelScale[0] + .5);
    }

    private int glHeight() {
        return (int) (getHeight() * pixelScale[1] + .5);
    }

    private void frameRendered() {
        fpsCount++;
    }

    private GLContext createExternalContext() {
        // Temporary migration bridge:
        // the render surface is LWJGL-backed, but the renderer and layer stack
        // still consume JOGL GL3. This wraps the current LWJGL context so the
        // existing renderer can keep running until the render path is ported.
        return GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL3)).createExternalGLContext();
    }

    private void ensureRendererInitialized() {
        if (rendererInitialized)
            return;

        try {
            if (externalContext == null)
                externalContext = createExternalContext();

            externalContext.makeCurrent();
            GL3 gl = externalContext.getGL().getGL3();
            initGLInfo(gl);
            GLRenderer.init(gl);
            rendererInitialized = true;
            externalContextInitDeferred = false;
            lastGlWidth = -1;
            lastGlHeight = -1;
        } catch (GLException e) {
            if (!externalContextInitDeferred) {
                Log.warn("Deferring JOGL external GL context initialization", e);
                externalContextInitDeferred = true;
            }
            if (externalContext != null) {
                externalContext.destroy();
                externalContext = null;
            }
        } finally {
            GLContext currentContext = externalContext;
            if (currentContext != null && currentContext.isCurrent())
                currentContext.release();
        }
    }

    private void disposeRenderer() {
        GLContext currentContext = externalContext;
        if (currentContext == null || context == 0L)
            return;

        try {
            runInContext(() -> {
                try {
                    currentContext.makeCurrent();
                    GLRenderer.dispose(currentContext.getGL().getGL3());
                } finally {
                    currentContext.release();
                    currentContext.destroy();
                    externalContext = null;
                    rendererInitialized = false;
                    externalContextInitDeferred = false;
                    lastGlWidth = -1;
                    lastGlHeight = -1;
                }
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to dispose OpenGL renderer", e);
        }
    }

    @Override
    public void reshape(int x, int y, int width, int height) {
        try {
            super.reshape(x, y, width, height);
        } catch (InternalError e) {
            Log.warn("Retrying AWTGLCanvas reshape after resize failure", e);
            EventQueue.invokeLater(() -> {
                revalidate();
                repaint();
            });
        }
    }

}
