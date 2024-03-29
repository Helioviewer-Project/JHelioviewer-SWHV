package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public interface GLSLVertexReceiver {

    void setVertexRepeatable(GL2 gl, BufVertex buf);

    default void setVertex(GL2 gl, BufVertex buf) { // default method clears buffer for safety
        setVertexRepeatable(gl, buf);
        buf.clear();
    }

}
