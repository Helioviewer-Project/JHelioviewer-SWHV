package org.helioviewer.jhv.opengl;

import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;

public class GLInfo {

    public static final int GLSAMPLES = 4;

    public static final double[] pixelScale = {1, 1};
    public static String glVersion = "";

    public static int maxTextureSize;

    static void glVersionError(String err) {
        Log.error(err);
        Message.fatalErr("OpenGL fatal error. JHelioviewer is not able to run:\n" + err);
    }

    public static void get(GL3 gl) {
        glVersion = "OpenGL " + gl.glGetString(GL3.GL_VERSION);
        Log.info(glVersion);
        // Log.info("Extensions: " + gl.glGetString(GL3.GL_EXTENSIONS));
        if (!gl.isExtensionAvailable("GL_VERSION_3_3")) {
            glVersionError("OpenGL 3.3 not supported.");
        }

        int[] out = {0};
        gl.glGetIntegerv(GL3.GL_MAX_TEXTURE_SIZE, out, 0);
        maxTextureSize = out[0];
    }

    public static void updatePixelScale(GLCanvas canvas) {
        GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
        if (gc != null) {
            AffineTransform tx = gc.getDefaultTransform();
            pixelScale[0] = tx.getScaleX();
            pixelScale[1] = tx.getScaleY();
        } else {
            pixelScale[0] = 1;
            pixelScale[1] = 1;
        }
    }

    public static boolean checkGLErrors(GL3 gl, String message) {
        int glErrorCode, errors = 0;

        while ((glErrorCode = gl.glGetError()) != GL3.GL_NO_ERROR) {
            Log.error("GL Error " + errorString(glErrorCode) + " (0x" + Integer.toHexString(glErrorCode) + ") - @" + message);
            errors++;
        }
        return errors != 0;
    }

    private static String errorString(int glErrorCode) {
        return switch (glErrorCode) {
            case GL3.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL3.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL3.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL3.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GL3.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            default -> "unknown";
        };
    }

}
