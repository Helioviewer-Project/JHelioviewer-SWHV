package org.helioviewer.swhv;

import javax.media.opengl.GL3;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4f;

public interface Camera {
    public void reshape(int w, int h);

    public GL3DMat4f getViewProjectionMatrix(GL3 gl);
}
