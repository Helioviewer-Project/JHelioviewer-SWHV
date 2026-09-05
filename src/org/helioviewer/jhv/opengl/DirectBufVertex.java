package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.util.List;

import org.helioviewer.jhv.base.BufferUtils;

public final class DirectBufVertex {

    private final ByteBuffer buffer;
    private final int count;

    public DirectBufVertex(BufVertex vertices) {
        buffer = copy(vertices.toBuffer());
        count = vertices.getCount();
    }

    public DirectBufVertex(List<BufVertex> vertices) {
        if (vertices.isEmpty())
            throw new IllegalArgumentException("Empty BufVertex list");

        int totalCount = 0;
        for (BufVertex source : vertices)
            totalCount = Math.addExact(totalCount, source.getCount());
        count = totalCount;
        buffer = BufferUtils.newByteBuffer(Math.multiplyExact(count, BufVertex.BYTES_PER_VERTEX));
        for (BufVertex source : vertices) {
            ByteBuffer data = source.toBuffer();
            BufferUtils.putRange(buffer, data, 0, data.limit());
        }
        buffer.flip();
    }

    private static ByteBuffer copy(ByteBuffer buffer) {
        ByteBuffer ret = BufferUtils.newByteBuffer(buffer.remaining());
        return BufferUtils.putRemaining(ret, buffer).flip();
    }

    ByteBuffer buffer() {
        return buffer;
    }

    int count() {
        return count;
    }

}
