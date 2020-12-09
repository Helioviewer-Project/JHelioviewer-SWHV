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

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLTexture {

    public enum Unit {
        ZERO, ONE, TWO, THREE
    }

    private int texID;
    private final int unit;
    private final int target;

    private int prev_width = -1;
    private int prev_height = -1;
    private int prev_inputGLFormat = -1;
    private int prev_bppGLType = -1;

    public GLTexture(GL2 gl, int _target, Unit _unit) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        texID = tmp[0];
        target = _target;
        unit = GL2.GL_TEXTURE0 + _unit.ordinal();
    }

    public void bind(GL2 gl) {
        gl.glActiveTexture(unit);
        gl.glBindTexture(target, texID);
    }

    public void delete(GL2 gl) {
        gl.glDeleteTextures(1, new int[]{texID}, 0);
        texID = prev_width = -1;
    }

    private static void genTexture2D(GL2 gl, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 0);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, buffer);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
    }

    public void copyImageBuffer(GL2 gl, ImageBuffer imageBuffer) {
        int w = imageBuffer.width;
        int h = imageBuffer.height;
        if (w < 1 || h < 1 || w > GLInfo.maxTextureSize || h > GLInfo.maxTextureSize) {
            Log.error("GLTexture.copyImageData2D: w= " + w + " h=" + h);
            return;
        }

        ImageBuffer.Format format = imageBuffer.format;
        int inputGLFormat = mapImageFormatToInputGLFormat(format);
        int bppGLType = mapBytesPerPixelToGLType(format.bytes);

        if (w != prev_width || h != prev_height || prev_inputGLFormat != inputGLFormat || prev_bppGLType != bppGLType) {
            int internalGLFormat = mapImageFormatToInternalGLFormat(format);
            genTexture2D(gl, internalGLFormat, w, h, inputGLFormat, bppGLType, null);

            prev_width = w;
            prev_height = h;
            prev_inputGLFormat = inputGLFormat;
            prev_bppGLType = bppGLType;
        }

        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, format.bytes);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, w);
        gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, w, h, inputGLFormat, bppGLType, imageBuffer.buffer);
    }

    public static void copyBufferedImage(GL2 gl, BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w < 1 || h < 1 || w > GLInfo.maxTextureSize || h > GLInfo.maxTextureSize) {
            Log.error("GLTexture.copyBufferedImage2D: w= " + w + " h=" + h);
            return;
        }

        DataBuffer rawBuffer = source.getRaster().getDataBuffer();
        Buffer buffer;

        switch (rawBuffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                buffer = ByteBuffer.wrap(((DataBufferByte) rawBuffer).getData());
                break;
            case DataBuffer.TYPE_USHORT:
                buffer = ShortBuffer.wrap(((DataBufferUShort) rawBuffer).getData());
                break;
            case DataBuffer.TYPE_SHORT:
                buffer = ShortBuffer.wrap(((DataBufferShort) rawBuffer).getData());
                break;
            case DataBuffer.TYPE_INT:
                buffer = IntBuffer.wrap(((DataBufferInt) rawBuffer).getData());
                break;
            default:
                buffer = null;
        }

        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, mapDataBufferTypeToGLAlign(rawBuffer.getDataType()));
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, w);
        genTexture2D(gl, mapTypeToInternalGLFormat(source.getType()), w, h, mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(rawBuffer.getDataType()), buffer);
    }

    public static void copyBuffer1D(GL2 gl, IntBuffer source) {
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);
        gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, GL2.GL_RGBA, source.limit(), 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, source);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
    }

    // Map application image format to OpenGL memory image format
    private static int mapImageFormatToInternalGLFormat(ImageBuffer.Format format) {
        switch (format) {
            case Gray8:
                return GL2.GL_R8;
            case Gray16:
                return GL2.GL_R16;
            case ARGB32:
                return GL2.GL_RGBA;
            default:
                throw new IllegalArgumentException("Format not supported");
        }
    }

    // Map application image format to OpenGL input image format
    private static int mapImageFormatToInputGLFormat(ImageBuffer.Format format) {
        switch (format) {
            case Gray8:
            case Gray16:
                return GL2.GL_RED;
            case ARGB32:
                return GL2.GL_BGRA;
            default:
                throw new IllegalArgumentException("Format not supported");
        }
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for saving the texture.
     *
     * @param type BufferedImage internal image format
     * @return OpenGL memory image format
     */
    private static int mapTypeToInternalGLFormat(int type) {
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED)
            return GL2.GL_R8;
        else
            return GL2.GL_RGBA;
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for transferring the texture.
     *
     * @param type BufferedImage internal image format
     * @return OpenGL input image format
     */
    private static int mapTypeToInputGLFormat(int type) {
        switch (type) {
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_INDEXED:
                return GL2.GL_RED;
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_ARGB:
                return GL2.GL_BGRA;
            default:
                return GL2.GL_RGBA;
        }
    }

    /**
     * Internal function to map the number of bytes per pixel to OpenGL types,
     * used for transferring the texture.
     */
    private static int mapBytesPerPixelToGLType(int bytesPerPixel) {
        switch (bytesPerPixel) {
            case 1:
                return GL2.GL_UNSIGNED_BYTE;
            case 2:
                return GL2.GL_UNSIGNED_SHORT;
            case 4:
                return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
            default:
                return 0;
        }
    }

    /**
     * Internal function to map the type of the a DataBuffer to OpenGL types,
     * used for transferring the texture.
     *
     * @param dataBufferType DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLType(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return GL2.GL_UNSIGNED_BYTE;
            case DataBuffer.TYPE_SHORT:
                return GL2.GL_SHORT;
            case DataBuffer.TYPE_USHORT:
                return GL2.GL_UNSIGNED_SHORT;
            case DataBuffer.TYPE_INT:
                return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
            default:
                return 0;
        }
    }

    /**
     * Internal function to map the type of the a DataBuffer to OpenGL aligns,
     * used for reading input data.
     *
     * @param dataBufferType DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLAlign(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return 1;
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
                return 2;
            case DataBuffer.TYPE_INT:
                return 4;
            default:
                return 0;
        }
    }

}
