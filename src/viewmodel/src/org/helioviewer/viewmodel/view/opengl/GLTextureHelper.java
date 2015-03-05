package org.helioviewer.viewmodel.view.opengl;

import java.awt.Rectangle;
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

import javax.media.opengl.GL2;

import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imageformat.ARGB32ImageFormat;
import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.RGB24ImageFormat;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.viewmodel.region.Region;

/**
 * Helper class to handle OpenGL textures.
 *
 * <p>
 * This class provides a lot of useful functions to handle textures in OpenGL,
 * including the generation of textures out of image data objects.
 *
 * @author Markus Langenberg
 */
public class GLTextureHelper {

    private final static int[] formatMap = { GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16 };

    private static boolean is3D = true;

    /**
     * Initializes the helper.
     *
     * This function has to be called before using any other helper function,
     * except {@link #setTextureNonPowerOfTwo(boolean)}, which should be called
     * before.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    public static void initHelper(GL2 gl) {
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View2D) {
            is3D = false;
        }
    }


    private static void genTexture2D(GL2 gl, int texID, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, buffer);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
    }

    /**
     * Renders the texture currently loaded to the given region.
     *
     * The given texture it is drawn onto a surface using the position and size
     * of the given region. That way, OpenGL handles the scaling and positioning
     * of different layers automatically.
     *
     * <p>
     * The texture has to be smaller than the maximum texture size
     * (GL_MAX_TEXTURE_SIZE).
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param region
     *            Position and size to draw the texture
     */
    private static void renderTextureToScreen(GL2 gl, Region region) {
        if (is3D) {
            return;
        }

        Vector2dDouble lowerleftCorner = region.getLowerLeftCorner();
        Vector2dDouble size = region.getSize();

        float x0 = (float) lowerleftCorner.getX();
        float y0 = (float) lowerleftCorner.getY();
        float x1 = x0 + (float) size.getX();
        float y1 = y0 + (float) size.getY();

        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex2f(x0, y0);
            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex2f(x1, y0);
            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex2f(x1, y1);
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex2f(x0, y1);
        }
        gl.glEnd();
        gl.glColorMask(true, true, true, true);
    }

    /**
     * Renders the content of the given image data object to the screen.
     *
     * The image data is moved to a temporary texture and it is drawn onto a
     * surface using the position and size of the given region. That way, OpenGL
     * handles the scaling and positioning of different layers automatically.
     *
     * <p>
     * If the given ImageData is bigger than the maximal texture size, multiple
     * tiles are drawn.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param region
     *            Position and size to draw the image
     * @param source
     *            Image data to draw to the screen
     */
    public static void renderImageDataToScreen(GL2 gl, Region region, ImageData source, GLTexture tex) {
        if (source == null || region == null)
            return;

        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= GLInfo.maxTextureSize && height <= GLInfo.maxTextureSize) {
            moveImageDataToGLTexture(gl, source, 0, 0, width, height, tex);
            renderTextureToScreen(gl, region);
        } else {
            Log.error(">> GLTextureHelper.renderImageDataToScreen(GL) > Image data too big: [" + width + "," + height + "]");
        }
    }

    /**
     * Saves a given image data object to a given texture.
     *
     * This version of the function provides the capabilities to specify a sub
     * region of the image data object, which is moved to the given texture.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param source
     *            Image data to copy to the texture
     * @param x
     *            x-offset of the sub region
     * @param y
     *            y-offset of the sub region
     * @param width
     *            width of the sub region
     * @param height
     *            height of the sub region
     * @param target
     *            Valid texture id
     */
    public static void moveImageDataToGLTexture(GL2 gl, ImageData source, int x, int y, int width, int height, GLTexture tex) {
        if (source == null)
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
        genTexture2D(gl, tex.get(gl), mapImageFormatToInternalGLFormat(imageFormat), width, height, mapImageFormatToInputGLFormat(imageFormat), mapBitsPerPixelToGLType(bitsPerPixel), buffer);
    }

    /**
     * Saves a given BufferedImage to a given texture.
     *
     * If it is possible to use
     * {@link #moveImageDataToGLTexture(GL2, ImageData, int)}, that function
     * should be preferred because the it is faster.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param source
     *            BufferedImage to copy to the texture
     * @param target
     *            Valid texture id
     * @see #moveImageDataToGLTexture(GL2, ImageData, int)
     */
    public static void moveBufferedImageToGLTexture(GL2 gl, BufferedImage source, GLTexture tex) {
        if (source == null)
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

        genTexture2D(gl, tex.get(gl), mapTypeToInternalGLFormat(source.getType()), source.getWidth(), source.getHeight(), mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(rawBuffer.getDataType()), buffer);
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

    public static class GLTexture {
        private int texID = -1;
        private GL2 gl;

        public int get(GL2 _gl) {
            if (texID == -1) {
                gl = _gl;

                int[] tmp = new int[1];
                gl.glGenTextures(1, tmp, 0);
                texID = tmp[0];
            }
            return texID;
        }

        protected void finalize() {
            if (texID != -1) {
                gl.glDeleteTextures(1, new int[] { texID }, 0);
            }
        }
    }

    private static int pixelHIFactorWidth = 1;

    public static void setPixelHIFactorWidth(int i) {
        pixelHIFactorWidth = i;
    }

    public static int getPixelHIFactorWidth() {
        return pixelHIFactorWidth;
    }

    private static int pixelHIFactorHeight = 1;

    public static void setPixelHIFactorHeight(int i) {
        pixelHIFactorHeight = i;
    }

    public static int getPixelHIFactorHeight() {
        return pixelHIFactorHeight;
    }
}
