package org.helioviewer.jhv.opengl;

interface GLSLVertexReceiver {

    void setVertexRepeatable(BufVertex vexBuf);

    default void setVertex(BufVertex vexBuf) { // default method clears buffer for safety
        setVertexRepeatable(vexBuf);
        vexBuf.clear();
    }

}
