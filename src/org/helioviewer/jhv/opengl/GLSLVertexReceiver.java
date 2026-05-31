package org.helioviewer.jhv.opengl;

interface GLSLVertexReceiver {

    void setVertexRepeatable(BufVertex vexBuf);

    void setVertexRepeatable(DirectBufVertex vexBuf);

    default void setVertex(BufVertex vexBuf) { // default method clears buffer for safety
        setVertexRepeatable(vexBuf);
        vexBuf.clear();
    }

}
