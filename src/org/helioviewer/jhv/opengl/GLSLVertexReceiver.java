package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

public interface GLSLVertexReceiver {

    void setVertexRepeatable(GL3 gl, BufVertex buf);

    default void setVertex(GL3 gl, BufVertex buf) { // default method clears buffer for safety
        setVertexRepeatable(gl, buf);
        buf.clear();
    }

}
