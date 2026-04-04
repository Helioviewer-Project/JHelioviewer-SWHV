package org.helioviewer.jhv.opengl;

import static org.lwjgl.opengl.GL11.GL_INVALID_ENUM;
import static org.lwjgl.opengl.GL11.GL_INVALID_OPERATION;
import static org.lwjgl.opengl.GL11.GL_INVALID_VALUE;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;

import org.helioviewer.jhv.Log;

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

        while ((glErrorCode = glGetError()) != GL_NO_ERROR) {
            Log.error("GL Error " + errorString(glErrorCode) + " (0x" + Integer.toHexString(glErrorCode) + ") - @" + message);
            errors++;
        }
        return errors != 0;
    }

    private static String errorString(int glErrorCode) {
        return switch (glErrorCode) {
            case GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            default -> "unknown";
        };
    }
}
