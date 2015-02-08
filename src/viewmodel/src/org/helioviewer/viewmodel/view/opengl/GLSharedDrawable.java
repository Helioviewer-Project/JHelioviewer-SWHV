package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

public class GLSharedDrawable {

    private static GLSharedDrawable instance = new GLSharedDrawable();

    public static GLSharedDrawable getSingletonInstance() {
        if (instance == null) {
            throw new NullPointerException("GLSharedDrawable not initialized");
        }
        return instance;
    }

    public final GLAutoDrawable sharedDrawable;
    public final GLCapabilitiesImmutable caps;

    public GLSharedDrawable() {
        final GLProfile glp = GLProfile.getDefault();
        caps = new GLCapabilities(glp);

        sharedDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, caps, null);
        sharedDrawable.display();
    }

}
