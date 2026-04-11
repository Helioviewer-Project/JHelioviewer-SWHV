package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;

class GLBO {

    private final int target;
    private final int bufferID;
    private final int usage;

    GLBO(int _target, int _usage) {
        target = _target;
        bufferID = GL.glGenBuffer();
        usage = _usage;
    }

    void delete() {
        GL.glDeleteBuffer(bufferID);
    }

    void bind() {
        GL.glBindBuffer(target, bufferID);
    }

    void setBufferData(int limit, int capacity, Buffer buffer) {
        GL.glBindBuffer(target, bufferID);
        GL.glBufferData(target, capacity, usage); // orphan, https://www.khronos.org/opengl/wiki/Buffer_Object_Streaming#Buffer_re-specification
        switch (buffer) {
            case ByteBuffer byteBuffer -> GL.glBufferSubData(target, 0, directByteBuffer(byteBuffer));
            case FloatBuffer floatBuffer -> GL.glBufferSubData(target, 0, directFloatBuffer(floatBuffer));
            case IntBuffer intBuffer -> GL.glBufferSubData(target, 0, directIntBuffer(intBuffer));
            case ShortBuffer shortBuffer -> GL.glBufferSubData(target, 0, directShortBuffer(shortBuffer));
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + buffer.getClass().getName());
        }
    }

    private static ByteBuffer directByteBuffer(ByteBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ByteBuffer copy = BufferUtils.newByteBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    private static FloatBuffer directFloatBuffer(FloatBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        FloatBuffer copy = BufferUtils.newFloatBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    private static IntBuffer directIntBuffer(IntBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        IntBuffer copy = BufferUtils.newIntBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    private static ShortBuffer directShortBuffer(ShortBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ShortBuffer copy = BufferUtils.newShortBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    int getID() {
        return bufferID;
    }

}
