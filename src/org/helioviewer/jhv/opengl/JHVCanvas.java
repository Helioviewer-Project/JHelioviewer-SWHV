package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;

import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

@SuppressWarnings("serial")
public final class JHVCanvas extends AWTGLCanvas implements RenderSurface {

    public static final int GLSAMPLES = 4;
    public static String glVersion = "";
    public static int maxTextureSize;

    private boolean whiteBackground;
    private boolean displayPending;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    private boolean rendererInitialized;
    private int lastGlWidth = -1;
    private int lastGlHeight = -1;

    private JHVCanvas(GLData data) {
        super(data);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Force a redraw after AWT resize so the GL pixel size is recomputed
                // immediately and the aspect ratio does not lag behind the canvas size.
                lastGlWidth = -1;
                lastGlHeight = -1;
                EventQueue.invokeLater(JHVCanvas.this::requestRender);
            }
        });
    }

    public static JHVCanvas create() {
        try {
            return new JHVCanvas(createData());
        } catch (Exception e) {
            throw glStartupError(e.getMessage() == null ? "Unknown OpenGL error." : e.getMessage());
        }
    }

    @Override
    public void initGL() {
        updatePixelScale();
        org.lwjgl.opengl.GL.createCapabilities();
        ensureRendererInitialized();
    }

    @Override
    public void paintGL() {
        updatePixelScale();
        ensureRendererInitialized();
        if (!rendererInitialized)
            return;

        int glWidth = (int) (getWidth() * Display.pixelScale[0] + .5);
        int glHeight = (int) (getHeight() * Display.pixelScale[1] + .5);
        if (glWidth != lastGlWidth || glHeight != lastGlHeight) {
            GLRenderer.reshape(glWidth, glHeight);
            lastGlWidth = glWidth;
            lastGlHeight = glHeight;
        }
        GLRenderer.display(whiteBackground);
        swapBuffers();

        Camera camera = Display.getCamera();
        if (Movie.isRecording())
            ExportMovie.handleMovieExport(camera);
        Layers.getViewpointLayer().updateTime(camera.getViewpoint().time);
        JHVFrame.getZoomStatusPanel().update(camera.getCameraWidth(), camera.getViewpoint().distance, Display.mode);
        fpsCount++;
    }

    @Override
    public void requestRender() {
        if (displayPending)
            return;

        displayPending = true;
        EventQueue.invokeLater(() -> {
            displayPending = false;
            render();
        });
    }

    @Override
    public void removeNotify() {
        disposeRenderer();
        super.removeNotify();
    }

    @Override
    public void setWhiteBackground(boolean whiteBackground) {
        this.whiteBackground = whiteBackground;
    }

    @Override
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

    private void ensureRendererInitialized() {
        if (rendererInitialized)
            return;

        initGLInfo();
        GLRenderer.init();
        rendererInitialized = true;
        lastGlWidth = -1;
        lastGlHeight = -1;
    }

    private void disposeRenderer() {
        if (!rendererInitialized || context == 0L)
            return;

        try {
            runInContext(() -> {
                GLRenderer.dispose();
                rendererInitialized = false;
                lastGlWidth = -1;
                lastGlHeight = -1;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to dispose OpenGL renderer", e);
        }
    }

    private void updatePixelScale() {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            Display.pixelScale[0] = 1;
            Display.pixelScale[1] = 1;
            return;
        }

        AffineTransform transform = graphicsConfiguration.getDefaultTransform();
        Display.pixelScale[0] = transform.getScaleX();
        Display.pixelScale[1] = transform.getScaleY();
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

    private static void initGLInfo() {
        glVersion = GL.formatVersionString(GL.glGetString(GL.VERSION));
        Log.info(glVersion);
        if (!org.lwjgl.opengl.GL.getCapabilities().OpenGL33)
            throw glStartupError("OpenGL 3.3 not supported.");

        maxTextureSize = GL.glGetInteger(GL.MAX_TEXTURE_SIZE);
    }

    private static AssertionError glStartupError(String err) {
        Log.error(err);
        Message.fatalErr("OpenGL fatal error. JHelioviewer is not able to run:\n" + err);
        return new AssertionError(err);
    }

}
