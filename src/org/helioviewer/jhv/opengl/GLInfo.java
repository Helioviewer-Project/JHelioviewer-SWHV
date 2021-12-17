package org.helioviewer.jhv.opengl;

import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

import com.jogamp.opengl.awt.GLCanvas;

public class GLInfo {

    public static final int GLSAMPLES = 4;

    public static final double[] pixelScale = {1, 1};
    public static String glVersion = "";

    public static int maxTextureSize;

    static void glVersionError(String err) {
        Log.error(err);
        Message.err("OpenGL fatal error, JHelioviewer is not able to run:\n", err, true);
    }

    public static void get(GL2 gl) {
        glVersion = "OpenGL " + gl.glGetString(GL2.GL_VERSION);
        Log.info(glVersion);
        // Log.info("Extensions: " + gl.glGetString(GL2.GL_EXTENSIONS));
        if (!gl.isExtensionAvailable("GL_VERSION_3_3")) {
            glVersionError("OpenGL 3.3 not supported.");
        }

        int[] out = {0};
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, out, 0);
        maxTextureSize = out[0];
    }

    public static void updatePixelScale(GLCanvas canvas) {
        GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
        if (gc != null) {
            AffineTransform tx = gc.getDefaultTransform();
            pixelScale[0] = tx.getScaleX();
            pixelScale[1] = tx.getScaleY();
        }
    }

    public static boolean checkGLErrors(GL2 gl, String message) {
        GLU glu = new GLU();
        int glErrorCode, errors = 0;

        while ((glErrorCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            errors++;
        }
        return errors != 0;
    }

}
