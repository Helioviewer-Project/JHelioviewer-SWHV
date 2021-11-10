package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public interface GLSLVertexReceiver {

    void setVertex(GL2 gl, BufVertex buf);

}
