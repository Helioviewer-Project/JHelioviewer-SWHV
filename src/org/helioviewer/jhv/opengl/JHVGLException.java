package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.Log;

import com.jogamp.opengl.GL3;

@SuppressWarnings("serial")
final class JHVGLException extends RuntimeException {

    JHVGLException(String message) {
        super(message);
    }

    JHVGLException(String message, Throwable cause) {
        super(message, cause);
    }

    static boolean checkErrors(GL3 gl, String message) {
        int glErrorCode;
        int errors = 0;

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
