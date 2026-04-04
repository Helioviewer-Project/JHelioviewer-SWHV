package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.imagedata.ImageBuffer;

import org.lwjgl.opengl.GL33;

public class GLTexture {

    public enum Unit {
        ZERO, ONE, TWO, THREE
    }

    private int texID;
    private final int unit;
    private final int target;
    private final GLBO pbo;

    private int prev_width = -1;
    private int prev_height = -1;
    private int prev_inputGLFormat = -1;
    private int prev_bppGLType = -1;

    public GLTexture(int _target, Unit _unit) {
        texID = GL33.glGenTextures();
        pbo = new GLBO(GL33.GL_PIXEL_UNPACK_BUFFER, GL33.GL_STREAM_DRAW);

        target = _target;
        unit = GL33.GL_TEXTURE0 + _unit.ordinal();
    }

    public void bind() {
        GL33.glActiveTexture(unit);
        GL33.glBindTexture(target, texID);
    }

    public void delete() {
        GL33.glDeleteTextures(texID);
        pbo.delete();
        texID = prev_width = -1;
    }

    private static void genTexture2D(int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_BASE_LEVEL, 0);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAX_LEVEL, 0);
        switch (buffer) {
            case null ->
                    GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, (ByteBuffer) null);
            case ByteBuffer byteBuffer ->
                    GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, byteBuffer);
            case ShortBuffer shortBuffer ->
                    GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, shortBuffer);
            case IntBuffer intBuffer ->
                    GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, intBuffer);
            default ->
                    throw new IllegalArgumentException("Unsupported texture buffer type: " + buffer.getClass().getName());
        }
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_LINEAR);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_LINEAR);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE);
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

        if (w != prev_width || h != prev_height || prev_inputGLFormat != inputGLFormat || prev_bppGLType != bppGLType) {
            int internalGLFormat = mapImageFormatToInternalGLFormat(format);
            genTexture2D(internalGLFormat, w, h, inputGLFormat, bppGLType, null);

            prev_width = w;
            prev_height = h;
            prev_inputGLFormat = inputGLFormat;
            prev_bppGLType = bppGLType;
        }

        GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, format.bytes);
        GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH, w);

        int size = format.bytes * imageBuffer.buffer.capacity();
        pbo.setBufferData(size, size, imageBuffer.buffer);
        GL33.glTexSubImage2D(GL33.GL_TEXTURE_2D, 0, 0, 0, w, h, inputGLFormat, bppGLType, 0L);
        GL33.glBindBuffer(GL33.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    public static void copyBufferedImage(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w < 1 || h < 1 || w > JHVCanvas.maxTextureSize || h > JHVCanvas.maxTextureSize) {
            Log.warn("w= " + w + " h=" + h);
            return;
        }

        DataBuffer rawBuffer = source.getRaster().getDataBuffer();
        int dataType = rawBuffer.getDataType();
        Buffer buffer = switch (dataType) {
            case DataBuffer.TYPE_BYTE -> ByteBuffer.wrap(((DataBufferByte) rawBuffer).getData());
            case DataBuffer.TYPE_USHORT -> ShortBuffer.wrap(((DataBufferUShort) rawBuffer).getData());
            case DataBuffer.TYPE_SHORT -> ShortBuffer.wrap(((DataBufferShort) rawBuffer).getData());
            case DataBuffer.TYPE_INT -> IntBuffer.wrap(((DataBufferInt) rawBuffer).getData());
            default -> null;
        };
        if (buffer == null) {
            Log.warn("Unsupported DataBuffer type: " + dataType);
            return;
        }

        buffer = directBuffer(buffer);

        GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, mapDataBufferTypeToGLAlign(dataType));
        GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH, w);
        genTexture2D(mapTypeToInternalGLFormat(source.getType()), w, h, mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(dataType), buffer);
    }

    private static Buffer directBuffer(Buffer buffer) {
        if (buffer.isDirect())
            return buffer;

        return switch (buffer) {
            case ByteBuffer byteBuffer -> {
                ByteBuffer direct = BufferUtils.newByteBuffer(byteBuffer.remaining());
                int position = byteBuffer.position();
                direct.put(byteBuffer);
                direct.flip();
                byteBuffer.position(position);
                yield direct;
            }
            case ShortBuffer shortBuffer -> {
                ShortBuffer direct = BufferUtils.newShortBuffer(shortBuffer.remaining());
                int position = shortBuffer.position();
                direct.put(shortBuffer);
                direct.flip();
                shortBuffer.position(position);
                yield direct;
            }
            case IntBuffer intBuffer -> {
                IntBuffer direct = BufferUtils.newIntBuffer(intBuffer.remaining());
                int position = intBuffer.position();
                direct.put(intBuffer);
                direct.flip();
                intBuffer.position(position);
                yield direct;
            }
            default ->
                    throw new IllegalArgumentException("Unsupported texture buffer type: " + buffer.getClass().getName());
        };
    }

    public static void copyBuffer1D(IntBuffer source) {
        GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, 4);
        GL33.glTexImage1D(GL33.GL_TEXTURE_1D, 0, GL33.GL_RGBA, source.capacity(), 0, GL33.GL_BGRA, GL33.GL_UNSIGNED_INT_8_8_8_8_REV, source);
        GL33.glTexParameteri(GL33.GL_TEXTURE_1D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_1D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_1D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
    }

    // map application image format to OpenGL memory image format
    private static int mapImageFormatToInternalGLFormat(ImageBuffer.Format format) {
        return switch (format) {
            case Gray8 -> GL33.GL_R8;
            case Gray16 -> GL33.GL_R16;
            case ARGB32 -> GL33.GL_RGBA;
        };
    }

    // Map application image format to OpenGL input image format
    private static int mapImageFormatToInputGLFormat(ImageBuffer.Format format) {
        return switch (format) {
            case Gray8, Gray16 -> GL33.GL_RED;
            case ARGB32 -> GL33.GL_BGRA;
        };
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for saving the texture.
     *
     * @param type BufferedImage internal image format
     * @return OpenGL memory image format
     */
    private static int mapTypeToInternalGLFormat(int type) {
        return type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED ? GL33.GL_R8 : GL33.GL_RGBA;
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for transferring the texture.
     *
     * @param type BufferedImage internal image format
     * @return OpenGL input image format
     */
    private static int mapTypeToInputGLFormat(int type) {
        return switch (type) {
            case BufferedImage.TYPE_BYTE_GRAY, BufferedImage.TYPE_BYTE_INDEXED -> GL33.GL_RED;
            case BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_INT_BGR, BufferedImage.TYPE_INT_ARGB -> GL33.GL_BGRA;
            default -> GL33.GL_RGBA;
        };
    }

    /**
     * Internal function to map the number of bytes per pixel to OpenGL types,
     * used for transferring the texture.
     */
    private static int mapBytesPerPixelToGLType(int bytesPerPixel) {
        return switch (bytesPerPixel) {
            case 1 -> GL33.GL_UNSIGNED_BYTE;
            case 2 -> GL33.GL_UNSIGNED_SHORT;
            case 4 -> GL33.GL_UNSIGNED_INT_8_8_8_8_REV;
            default -> 0;
        };
    }

    /**
     * Internal function to map the type of the DataBuffer to OpenGL types,
     * used for transferring the texture.
     *
     * @param dataBufferType DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLType(int dataBufferType) {
        return switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE -> GL33.GL_UNSIGNED_BYTE;
            case DataBuffer.TYPE_SHORT -> GL33.GL_SHORT;
            case DataBuffer.TYPE_USHORT -> GL33.GL_UNSIGNED_SHORT;
            case DataBuffer.TYPE_INT -> GL33.GL_UNSIGNED_INT_8_8_8_8_REV;
            default -> 0;
        };
    }

    /**
     * Internal function to map the type of the DataBuffer to OpenGL aligns,
     * used for reading input data.
     *
     * @param dataBufferType DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLAlign(int dataBufferType) {
        return switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE -> 1;
            case DataBuffer.TYPE_SHORT, DataBuffer.TYPE_USHORT -> 2;
            case DataBuffer.TYPE_INT -> 4;
            default -> 0;
        };
    }

}
