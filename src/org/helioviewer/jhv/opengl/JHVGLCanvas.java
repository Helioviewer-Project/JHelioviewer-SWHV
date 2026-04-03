package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import org.helioviewer.jhv.Log;

import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.awt.GLCanvas;

@SuppressWarnings("serial")
final class JHVGLCanvas extends GLCanvas {

    JHVGLCanvas(GLCapabilitiesImmutable capabilities) {
        super(capabilities);
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

    private static boolean isIncompleteResize(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (current instanceof InvocationTargetException ite) {
                current = ite.getTargetException();
            }
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
