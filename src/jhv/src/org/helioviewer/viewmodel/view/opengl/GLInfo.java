package org.helioviewer.viewmodel.view.opengl;

import org.helioviewer.base.logging.Log;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.glu.GLU;

public class GLInfo {

    public static int maxTextureSize;
    public static int[] pixelScale = new int[] { 1, 1 };

    public static void update(GL2 gl) throws GLException {
        String version = gl.glGetString(GL2.GL_VERSION);
        Log.debug(">> GLInfo > Version string: " + version);
        String extensionStr = gl.glGetString(GL2.GL_EXTENSIONS);
        Log.debug(">> GLInfo.update(GL) > Extensions: " + extensionStr);

        if (!gl.isExtensionAvailable("GL_VERSION_2_1")) {
            String err = "OpenGL 2.1 not supported. JHelioviewer is not able to run.";
            Log.error(">> GLInfo.update(GL) > " + err);
            throw new GLException(err);
        }

        int[] out = new int[1];

        out[0] = 0;
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, out, 0);
        maxTextureSize = out[0];
        Log.debug(">> GLInfo > max texture size: " + out[0]);
    }

    public static void updatePixelScale(ScalableSurface surface) {
        float[] scale = new float[2];

        surface.getCurrentSurfaceScale(scale);
        pixelScale[0] = (int) scale[0];
        pixelScale[1] = (int) scale[1];
    }

    public static boolean checkGLErrors(GL2 gl, String message) {
        GLU glu = new GLU();
        int glErrorCode, errors = 0;

        while ((glErrorCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            errors++;
        }

        if (errors == 0)
            return false;
        else
            return true;
    }

}
