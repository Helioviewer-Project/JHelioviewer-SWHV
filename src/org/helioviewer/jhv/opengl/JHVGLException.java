package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.app.Log;

@SuppressWarnings("serial")
final class JHVGLException extends RuntimeException {

    JHVGLException(String message) {
        super(message);
    }

    JHVGLException(String message, Throwable cause) {
        super(message, cause);
    }

    static boolean checkErrors(String message) {
        int glErrorCode;
        int errors = 0;

        while ((glErrorCode = GL.glGetError()) != GL.NO_ERROR) {
            Log.error("GL Error " + errorString(glErrorCode) + " (0x" + Integer.toHexString(glErrorCode) + ") - @" + message);
            errors++;
        }
        return errors != 0;
    }

    private static String errorString(int glErrorCode) {
        return switch (glErrorCode) {
            case GL.INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL.INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL.INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL.INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GL.OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            default -> "unknown";
        };
    }
}
