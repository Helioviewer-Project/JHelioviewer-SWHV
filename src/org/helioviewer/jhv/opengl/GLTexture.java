package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.imagedata.ImageBuffer;

public class GLTexture {

    public enum Unit {
        ZERO, ONE, TWO, THREE
    }

    private int texID;
    private final int unit;
    private final int target;
    private final GLBO pbo;

    private int previousWidth = -1;
    private int previousHeight = -1;
    private int previousInputFormat = -1;
    private int previousInputType = -1;

    public GLTexture(int textureTarget, Unit textureUnit) {
        texID = GL.glGenTexture();
        pbo = new GLBO(GL.PIXEL_UNPACK_BUFFER, GL.STREAM_DRAW);

        target = textureTarget;
        unit = GL.TEXTURE0 + textureUnit.ordinal();
    }

    public void bind() {
        GL.glActiveTexture(unit);
        GL.glBindTexture(target, texID);
    }

    public void delete() {
        GL.glDeleteTexture(texID);
        pbo.delete();
        texID = -1;
        previousWidth = -1;
        previousHeight = -1;
        previousInputFormat = -1;
        previousInputType = -1;
    }

    private static void genTexture2D(int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_BASE_LEVEL, 0);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 0);
        switch (buffer) {
            case null ->
                    GL.glTexImage2D(GL.TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, (ByteBuffer) null);
            case ByteBuffer byteBuffer ->
                    GL.glTexImage2D(GL.TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, directByteBuffer(byteBuffer));
            case ShortBuffer shortBuffer ->
                    GL.glTexImage2D(GL.TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, directShortBuffer(shortBuffer));
            default ->
                    throw new IllegalArgumentException("Unsupported texture buffer type: " + buffer.getClass().getName());
        }
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
    }

    public void copyImageBuffer(ImageBuffer imageBuffer) {
        int w = imageBuffer.width;
        int h = imageBuffer.height;
        if (w < 1 || h < 1 || w > JHVCanvas.maxTextureSize || h > JHVCanvas.maxTextureSize) {
            Log.warn("w= " + w + " h=" + h);
            return;
        }

        ImageBuffer.Format format = imageBuffer.format;
        int inputGLFormat = mapImageFormatToInputGLFormat(format);
        int bppGLType = mapBytesPerPixelToGLType(format.bytes);

        if (w != previousWidth || h != previousHeight || previousInputFormat != inputGLFormat || previousInputType != bppGLType) {
            int internalGLFormat = mapImageFormatToInternalGLFormat(format);
            genTexture2D(internalGLFormat, w, h, inputGLFormat, bppGLType, null);

            previousWidth = w;
            previousHeight = h;
            previousInputFormat = inputGLFormat;
            previousInputType = bppGLType;
        }

        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, format.bytes);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, w);

        int size = format.bytes * imageBuffer.buffer.capacity();
        pbo.setBufferData(size, size, imageBuffer.buffer);
        GL.glTexSubImage2D(GL.TEXTURE_2D, 0, 0, 0, w, h, inputGLFormat, bppGLType, 0L);
        GL.glBindBuffer(GL.PIXEL_UNPACK_BUFFER, 0);
    }

    public static void copyByteImage(int w, int h, ByteBuffer source) {
        if (w < 1 || h < 1 || w > JHVCanvas.maxTextureSize || h > JHVCanvas.maxTextureSize) {
            Log.warn("w= " + w + " h=" + h);
            return;
        }
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, w);
        genTexture2D(GL.RGBA, w, h, GL.RGBA, GL.UNSIGNED_BYTE, source);
    }

    public static void copyBuffer1D(ByteBuffer source) {
        source = directByteBuffer(source);
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_BASE_LEVEL, 0);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 0);
        GL.glTexImage2D(GL.TEXTURE_2D, 0, GL.RGBA, source.remaining() / 4, 1, 0, GL.RGBA, GL.UNSIGNED_BYTE, source);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.NEAREST);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.NEAREST);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
    }

    private static ByteBuffer directByteBuffer(ByteBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ByteBuffer direct = BufferUtils.newByteBuffer(buffer.remaining());
        direct.put(buffer.duplicate());
        direct.flip();
        return direct;
    }

    private static ShortBuffer directShortBuffer(ShortBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ShortBuffer direct = BufferUtils.newShortBuffer(buffer.remaining());
        direct.put(buffer.duplicate());
        direct.flip();
        return direct;
    }

    private static int mapImageFormatToInternalGLFormat(ImageBuffer.Format format) {
        return switch (format) {
            case Gray8 -> GL.R8;
            case Gray16 -> GL.R16;
            case RGBA32 -> GL.RGBA;
        };
    }

    private static int mapImageFormatToInputGLFormat(ImageBuffer.Format format) {
        return switch (format) {
            case Gray8, Gray16 -> GL.RED;
            case RGBA32 -> GL.RGBA;
        };
    }

    private static int mapBytesPerPixelToGLType(int bytesPerPixel) {
        return switch (bytesPerPixel) {
            case 1, 4 -> GL.UNSIGNED_BYTE;
            case 2 -> GL.UNSIGNED_SHORT;
            default -> 0;
        };
    }

}
