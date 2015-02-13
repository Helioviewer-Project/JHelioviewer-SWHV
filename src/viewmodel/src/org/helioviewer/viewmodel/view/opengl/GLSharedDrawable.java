package org.helioviewer.viewmodel.view.opengl;

import java.awt.Dimension;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

public class GLSharedDrawable {

    private final static GLSharedDrawable instance = new GLSharedDrawable();

    public static GLSharedDrawable getSingletonInstance() {
        return instance;
    }

    private static GLCapabilities caps;
    private static GLAutoDrawable sharedDrawable;

    private GLSharedDrawable() {
        final GLProfile glp = GLProfile.getDefault();
        caps = new GLCapabilities(glp);

        sharedDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, caps, null);
        sharedDrawable.display();
    }

    public GLCanvas getCanvas() {
        GLCanvas canvas = new GLCanvas(caps);
        canvas.setSharedAutoDrawable(sharedDrawable);
        canvas.setMinimumSize(new Dimension(0, 0));

        return canvas;
    }

}
