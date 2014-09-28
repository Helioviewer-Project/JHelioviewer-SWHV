package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GLContext;

/**
 * Class to manage the OpenGL context, in case of multiple OpenGL elements in
 * the GUI.
 *
 * <p>
 * Since OpenGL manages resources separately per context, having multiple
 * contexts might result in unpredictable behavior. To avoid that, all OpenGl
 * GUI elements should share one context.
 *
 * @author Markus Langenberg
 *
 */
public class GLSharedContext {
    private static GLContext context = null;

    /**
     * Sets the shared OpenGL context.
     *
     * This functions assign a new context only one time, every following call
     * is ignored, unless it is being tried to reset to null.
     *
     *
     * @param sharedContext
     */
    public static void setSharedContext(GLContext sharedContext) {
        if (context == null || sharedContext == null) {
            context = sharedContext;
        }
    }

    /**
     * Returns the shared OpenGL context.
     *
     * @return Shared context
     */
    public static GLContext getSharedContext() {
        return context;
    }

}
