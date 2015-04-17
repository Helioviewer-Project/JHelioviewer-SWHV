package org.helioviewer.viewmodel.view.opengl;

import java.awt.Dimension;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

public class GLSharedDrawable {

    private static GLCanvas canvas;
    private final static GLSharedDrawable instance = new GLSharedDrawable();

    public static GLSharedDrawable getSingletonInstance() {
        return instance;
    }

    private GLSharedDrawable() {
        canvas = new GLCanvas(new GLCapabilities(GLProfile.getDefault()));
        canvas.setMinimumSize(new Dimension(0, 0));
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

}
