package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

public interface GLSLVertexReceiver {

    void setVertexRepeatable(GL3 gl, BufVertex vexBuf);

    default void setVertex(GL3 gl, BufVertex vexBuf) { // default method clears buffer for safety
        setVertexRepeatable(gl, vexBuf);
        vexBuf.clear();
    }

}
