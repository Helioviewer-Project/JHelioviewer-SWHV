package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.BufferUtils;

public final class DirectBufVertex {

    private final ByteBuffer vertexBuffer;
    private final ByteBuffer colorBuffer;
    private final int count;

    public DirectBufVertex(BufVertex buf) {
        vertexBuffer = copy(buf.toVertexBuffer(), buf.vertexByteLength());
        colorBuffer = copy(buf.toColorBuffer(), buf.colorByteLength());
        count = buf.getCount();
    }

    private static ByteBuffer copy(Buffer buffer, int size) {
        ByteBuffer ret = BufferUtils.newByteBuffer(size);
        ret.put(((ByteBuffer) buffer).duplicate());
        return ret.flip();
    }

    ByteBuffer vertexBuffer() {
        return vertexBuffer;
    }

    ByteBuffer colorBuffer() {
        return colorBuffer;
    }

    int count() {
        return count;
    }

}
