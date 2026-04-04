package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import org.helioviewer.jhv.Log;

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

    private boolean whiteBack;
    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    public static JHVCanvas create() {
        JHVCanvas canvas = null;
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            GLCapabilities capabilities = getCapabilities(profile);
            canvas = new JHVCanvas(capabilities);
            GLRenderer renderer = new GLRenderer(canvas);
            canvas.addGLEventListener(new GLEventListener() {
                @Override
                public void init(GLAutoDrawable drawable) {
                    renderer.init((GL3) drawable.getGL());
                }

                @Override
                public void dispose(GLAutoDrawable drawable) {
                    renderer.dispose((GL3) drawable.getGL());
                }

                @Override
                public void display(GLAutoDrawable drawable) {
                    renderer.display((GL3) drawable.getGL());
                }

                @Override
                public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                    renderer.reshape(x, y, width, height);
                }
            });
            // GUI events can lead to context destruction and invalidation of GL objects and state
            canvas.setSharedAutoDrawable(getSharedDrawable(profile, capabilities));
        } catch (Exception e) {
            String msg = e.getMessage();
            GLInfo.glVersionError(msg == null ? "Unknown OpenGL error." : msg);
        }
        return canvas;
    }

    private JHVCanvas(GLCapabilitiesImmutable capabilities) {
        super(capabilities);
    }

    public boolean isWhiteBackground() {
        return whiteBack;
    }

    public void setWhiteBackground(boolean whiteBackground) {
        whiteBack = whiteBackground;
    }

    void frameRendered() {
        fpsCount++;
    }

    public int getFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - fpsTime;

        if (delta > 1000) {
            fps = (int) ((1000L * fpsCount + delta / 2) / delta);
            fpsCount = 0;
            fpsTime = currentTime;
        }
        return fps;
    }

    @Override
    public void reshape(int x, int y, int width, int height) {
        try {
            super.reshape(x, y, width, height);
        } catch (InternalError e) {
            if (!isIncompleteResize(e))
                throw e;
            Log.warn("Retrying GLCanvas reshape after incomplete resize operation", e);
            EventQueue.invokeLater(() -> {
                revalidate();
                repaint();
            });
        }
    }

    private static GLCapabilities getCapabilities(GLProfile profile) {
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(GLInfo.GLSAMPLES);
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
            if (resizeStack && message != null && message.startsWith("Incomplete resize operation:"))
                return true;
        }
        return false;
    }

}
