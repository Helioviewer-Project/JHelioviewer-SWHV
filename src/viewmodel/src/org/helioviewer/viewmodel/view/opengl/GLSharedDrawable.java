package org.helioviewer.viewmodel.view.opengl;

import java.awt.Dimension;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

public class GLSharedDrawable {

    private static GLSharedDrawable instance = new GLSharedDrawable();
    private final GLCanvas canvas;

    public static GLSharedDrawable getSingletonInstance() {
        if (instance == null) {
            throw new NullPointerException("GLSharedDrawable not initialized");
        }
        return instance;
    }

    public GLSharedDrawable() {
        final GLProfile glp = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glp);

        canvas = new GLCanvas(caps);
        canvas.setMinimumSize(new Dimension(0, 0));
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

}
