package org.helioviewer.viewmodel.view.opengl;

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

import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imageformat.ARGB32ImageFormat;
import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.RGB24ImageFormat;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;

import com.jogamp.opengl.GL2;

/**
 * Helper class to handle OpenGL textures.
 *
 * <p>
 * This class provides a lot of useful functions to handle textures in OpenGL,
 * including the generation of textures out of image data objects.
 *
 * @author Markus Langenberg
 */
public class GLTexture {

    private final static int[] formatMap = { GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16 };

    private int texID = -1;

    private int prev_width = -1;
    private int prev_height = -1;
    private int prev_inputGLFormat = -1;
    private int prev_bppGLType = -1;

    public GLTexture(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        texID = tmp[0];
    }

    public void bind(GL2 gl, int target) {
        gl.glBindTexture(target, texID);
    }

    public void delete(GL2 gl) {
        gl.glDeleteTextures(1, new int[] { texID }, 0);
        texID = prev_width = -1;
    }

    private static void genTexture2D(GL2 gl, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, buffer);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
    }

    public void copyImageData2D(GL2 gl, ImageData source, int x, int y, int width, int height) {
        if (width > GLInfo.maxTextureSize || height > GLInfo.maxTextureSize)
            return;

        int bitsPerPixel = source.getImageTransport().getNumBitsPerPixel();
        Buffer buffer;

        switch (bitsPerPixel) {
        case 8:
            buffer = ByteBuffer.wrap(((Byte8ImageTransport) source.getImageTransport()).getByte8PixelData());
            break;
        case 16:
            buffer = ShortBuffer.wrap(((Short16ImageTransport) source.getImageTransport()).getShort16PixelData());
            break;
        case 32:
            buffer = IntBuffer.wrap(((Int32ImageTransport) source.getImageTransport()).getInt32PixelData());
            break;
        default:
            buffer = null;
        }

        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, x);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, y);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, source.getWidth());
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

        ImageFormat imageFormat = source.getImageFormat();
        int inputGLFormat = mapImageFormatToInputGLFormat(imageFormat);
        int bppGLType = mapBitsPerPixelToGLType(bitsPerPixel);

        if (width != prev_width || height != prev_height || prev_inputGLFormat != inputGLFormat || prev_bppGLType != bppGLType) {
            int internalGLFormat = mapImageFormatToInternalGLFormat(imageFormat);
            genTexture2D(gl, internalGLFormat, width, height, inputGLFormat, bppGLType, null);

            prev_width = width;
            prev_height = height;
            prev_inputGLFormat = inputGLFormat;
            prev_bppGLType = bppGLType;
        }
        gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, width, height, inputGLFormat, bppGLType, buffer);
    }

    public void copyBufferedImage2D(GL2 gl, BufferedImage source) {
        int width, height;
        if ((width = source.getWidth()) > GLInfo.maxTextureSize || (height = source.getHeight()) > GLInfo.maxTextureSize)
            return;

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

        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, mapDataBufferTypeToGLAlign(rawBuffer.getDataType()));

        genTexture2D(gl, mapTypeToInternalGLFormat(source.getType()), width, height, mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(rawBuffer.getDataType()), buffer);
    }

    public void copyBuffer1D(GL2 gl, IntBuffer source) {
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);

        gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, GL2.GL_RGBA, source.limit(), 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, source);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
    }

    /**
     * Internal function to map the application internal image formats to OpenGL
     * image formats, used for saving the texture.
     *
     * @param imageFormat
     *            Application internal image format
     * @return OpenGL memory image format
     */
    private static int mapImageFormatToInternalGLFormat(ImageFormat imageFormat) {
        if (imageFormat instanceof SingleChannelImageFormat)
            return formatMap[((SingleChannelImageFormat) imageFormat).getBitDepth() - 1];
        else if (imageFormat instanceof ARGB32ImageFormat || imageFormat instanceof RGB24ImageFormat)
            return GL2.GL_RGBA;
        else
            throw new IllegalArgumentException("Format is not supported");
    }

    /**
     * Internal function to map the application internal image formats to OpenGL
     * image formats, used for transferring the texture.
     *
     * @param imageFormat
     *            Application internal image format
     * @return OpenGL input image format
     */
    private static int mapImageFormatToInputGLFormat(ImageFormat imageFormat) {
        if (imageFormat instanceof SingleChannelImageFormat)
            return GL2.GL_LUMINANCE;
        else if (imageFormat instanceof ARGB32ImageFormat || imageFormat instanceof RGB24ImageFormat)
            return GL2.GL_BGRA;
        else
            throw new IllegalArgumentException("Format is not supported");
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for saving the texture.
     *
     * @param type
     *            BufferedImage internal image format
     * @return OpenGL memory image format
     */
    private static int mapTypeToInternalGLFormat(int type) {
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED)
            return GL2.GL_LUMINANCE;
        else
            return GL2.GL_RGBA;
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for transferring the texture.
     *
     * @param type
     *            BufferedImage internal image format
     * @return OpenGL input image format
     */
    private static int mapTypeToInputGLFormat(int type) {
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED)
            return GL2.GL_LUMINANCE;
        else if (type == BufferedImage.TYPE_4BYTE_ABGR || type == BufferedImage.TYPE_INT_BGR || type == BufferedImage.TYPE_INT_ARGB)
            return GL2.GL_BGRA;
        else
            return GL2.GL_RGBA;
    }

    /**
     * Internal function to map the number of bits per pixel to OpenGL types,
     * used for transferring the texture.
     *
     * @param bitsPerPixel
     *            Bits per pixel of the input data
     * @return OpenGL type to use
     */
    private static int mapBitsPerPixelToGLType(int bitsPerPixel) {
        switch (bitsPerPixel) {
        case 8:
            return GL2.GL_UNSIGNED_BYTE;
        case 16:
            return GL2.GL_UNSIGNED_SHORT;
        case 32:
            return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        default:
            return 0;
        }
    }

    /**
     * Internal function to map the type of the a DataBuffer to OpenGL types,
     * used for transferring the texture.
     *
     * @param dataBufferType
     *            DataBuffer type of the input data
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
     * @param dataBufferType
     *            DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLAlign(int dataBufferType) {
        switch (dataBufferType) {
        case DataBuffer.TYPE_BYTE:
            return 1;
        case DataBuffer.TYPE_SHORT:
            return 2;
        case DataBuffer.TYPE_USHORT:
            return 2;
        case DataBuffer.TYPE_INT:
            return 4;
        default:
            return 0;
        }
    }

}
