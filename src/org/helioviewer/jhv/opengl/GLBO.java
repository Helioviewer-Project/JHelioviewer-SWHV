package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;

class GLBO {

    private final int target;
    private final int usage;

    private int bufferID;
    private float[] lastFloatData;

    GLBO(int _target, int _usage) {
        target = _target;
        bufferID = GL.glGenBuffer();
        usage = _usage;
    }

    void delete() {
        if (bufferID == -1)
            return;
        GL.glDeleteBuffer(bufferID);
        bufferID = -1;
        lastFloatData = null;
    }

    void bind() {
        GL.glBindBuffer(target, bufferID);
    }

    void setBufferData(int capacity, Buffer buffer) {
        GL.glBindBuffer(target, bufferID);
        GL.glBufferData(target, capacity, usage); // orphan, https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        switch (buffer) {
            case ByteBuffer byteBuffer -> GL.glBufferSubData(target, 0, BufferUtils.directByteBuffer(byteBuffer));
            case FloatBuffer floatBuffer -> GL.glBufferSubData(target, 0, BufferUtils.directFloatBuffer(floatBuffer));
            case IntBuffer intBuffer -> GL.glBufferSubData(target, 0, BufferUtils.directIntBuffer(intBuffer));
            case ShortBuffer shortBuffer -> GL.glBufferSubData(target, 0, BufferUtils.directShortBuffer(shortBuffer));
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + buffer.getClass().getName());
        }
    }

    void setBufferDataIfChanged(int capacity, FloatBuffer buffer) {
        int count = buffer.remaining();
        if (lastFloatData != null && lastFloatData.length == count && floatDataMatches(buffer, count))
            return;

        setBufferData(capacity, buffer);

        if (lastFloatData == null || lastFloatData.length != count)
            lastFloatData = new float[count];
        buffer.get(0, lastFloatData);
    }

    private boolean floatDataMatches(FloatBuffer buffer, int count) {
        for (int i = 0; i < count; i++) {
            if (Float.floatToRawIntBits(lastFloatData[i]) != Float.floatToRawIntBits(buffer.get(i)))
                return false;
        }
        return true;
    }

    int getID() {
        return bufferID;
    }

}
