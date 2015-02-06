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
import java.util.HashMap;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

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
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;

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
    public static boolean is2DState = false;
    private static TextureImplementation textureImplementation = null;
    private static boolean textureNonPowerOfTwo = false;
    private static int maxTextureSize = 2048;

    private static GLTextureCoordinate mainTexCoord = new GLMainTextureCoordinate();
    private static GLTextureCoordinate scaleTexCoord = new GLScaleTextureCoordinate();
    public static double scaleX;
    public static double scaleY;

    private static HashMap<Integer, Vector2dDouble> allTextures = new HashMap<Integer, Vector2dDouble>();

    private final static int[] formatMap = { GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE4, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE8, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE12, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16, GL2.GL_LUMINANCE16 };

    /**
     * Sets whether non power of two textures should be used.
     *
     * If non power of two textures are deactivated, they are simulated by using
     * parts of power of two textures. On some architectures, this is faster,
     * some architectures do not even support non power of two textures. In any
     * case, the memory use is higher for power of two textures.
     *
     * @param nonPowerOfTwo
     *            true for using non power of two textures, false otherwise.
     * @see #textureNonPowerOfTwoAvailable()
     */
    public static void setTextureNonPowerOfTwo(boolean nonPowerOfTwo) {
        if (nonPowerOfTwo != textureNonPowerOfTwo) {
            if (nonPowerOfTwo)
                textureImplementation = new NonPowerOfTwoTextureImplementation();
            else
                textureImplementation = new PowerOfTwoTextureImplementation();
        }

        textureNonPowerOfTwo = nonPowerOfTwo;
    }

    /**
     * Returns whether non power of two textures are activated.
     *
     * If non power of two textures are deactivated, they are simulated by using
     * parts of power of two textures. On some architectures, this is faster,
     * some architectures do not even support non power of two textures. In any
     * case, the memory use is higher for power of two textures.
     *
     * @return true if non power of two textures are activated, false otherwise.
     * @see #setTextureNonPowerOfTwo(boolean)
     */
    public static boolean textureNonPowerOfTwoAvailable() {
        return textureNonPowerOfTwo;
    }

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
        Log.debug(">> GLTextureHelper.initHelper(GL) > Initialize helper functions");

        if (textureImplementation == null) {
            Log.debug(">> GLTextureHelper.initHelper(GL) > Use Non-Power-Of-Two-Textures: " + textureNonPowerOfTwoAvailable());

            if (textureNonPowerOfTwoAvailable())
                textureImplementation = new NonPowerOfTwoTextureImplementation();
            else
                textureImplementation = new PowerOfTwoTextureImplementation();
        }

        int tmp[] = new int[1];
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
        maxTextureSize = tmp[0];
        Log.debug(">> GLTextureHelper.initHelper(GL) > max texture size: " + maxTextureSize);

        scaleTexCoord.setValue(gl, 1.0f, 1.0f);
    }

    /**
     * Generates a new texture.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @return new texture id
     */
    public int genTextureID(GL2 gl) {
        synchronized (allTextures) {
            int[] tmp = new int[1];
            gl.glGenTextures(1, tmp, 0);
            allTextures.put(tmp[0], null);
            return tmp[0];
        }
    }

    /**
     * Binds a 2D texture.
     *
     * If PowerOfTextures are used, this will also load the texture scaling to
     * the texture coordinate GL_TEXTURE1, which will be used by
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
     * . Thus, it is highly recommended to use this function for GL_TEXTURE0
     * instead of calling OpenGL directly.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param texture
     *            texture id to bind
     * @see #bindTexture(GL2, int, int)
     */
    public void bindTexture(GL2 gl, int texture) {
        synchronized (allTextures) {
            textureImplementation.bindTexture(gl, texture);
        }
    }

    /**
     * Binds a texture.
     *
     * If PowerOfTextures are used, this will also load the texture scaling to
     * the texture coordinate GL_TEXTURE1, which will be used by
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
     * . Thus, it is highly recommended to use this function for GL_TEXTURE0
     * instead of calling OpenGL directly.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param target
     *            texture type, such as GL_TEXTURE_2D
     * @param texture
     *            texture id to bind
     * @see #bindTexture(GL2, int, int)
     */
    public void bindTexture(GL2 gl, int target, int texture) {
        synchronized (allTextures) {
            textureImplementation.bindTexture(gl, target, texture);
        }
    }

    /**
     * Deletes an existing texture.
     *
     * It is not possible to delete a texture that has not been generated by
     * this helper.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param texID
     *            Texture id to delete
     */
    public void delTextureID(GL2 gl, int texID) {
        synchronized (allTextures) {

            if (!allTextures.containsKey(texID))
                return;

            if (gl == null) {
                gl = (GL2) GLU.getCurrentGL();
            }

            allTextures.remove(texID);

            int[] tmp = new int[1];
            tmp[0] = texID;
            gl.glDeleteTextures(1, tmp, 0);
        }
    }

    /**
     * Deletes all textures generates by this helper.
     *
     * This might be necessary to clean up after not using OpenGL any more.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    public void delAllTextures(GL2 gl) {
        synchronized (allTextures) {
            GLCommonRenderGraphics.clearImageTextureBuffer(gl);
            GLCommonRenderGraphics.clearStringTextureBuffer(gl);

            if (!allTextures.keySet().isEmpty()) {
                int[] textureIDs = new int[allTextures.keySet().size()];

                int i = 0;
                for (Integer copy : allTextures.keySet()) {
                    textureIDs[i++] = copy;
                }

                for (int textureID : textureIDs) {
                    delTextureID(gl, textureID);
                }
            }
        }
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
        Vector2dDouble lowerleftCorner = region.getLowerLeftCorner();
        Vector2dDouble size = region.getSize();

        float x0 = (float) lowerleftCorner.getX();
        float y0 = (float) lowerleftCorner.getY();
        float x1 = x0 + (float) size.getX();
        float y1 = y0 + (float) size.getY();
        renderTextureToScreen(gl, x0, y0, x1, y1);
    }

    /**
     * Renders the texture currently loaded to given rectangle.
     *
     * The rectangle is specified by its lower left and upper right corner.
     *
     * <p>
     * The texture has to be smaller than the maximum texture size
     * (GL_MAX_TEXTURE_SIZE).
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param x0
     *            , y0 , x1 , y1 - Position and size to draw the texture
     */
    private static void renderTextureToScreen(GL2 gl, float x0, float y0, float x1, float y1) {
        if (is2DState) {
            gl.glBegin(GL2.GL_QUADS);
            mainTexCoord.setValue(gl, 0.0f, 1.0f);
            gl.glVertex2f(x0, y0);
            mainTexCoord.setValue(gl, 1.0f, 1.0f);
            gl.glVertex2f(x1, y0);
            mainTexCoord.setValue(gl, 1.0f, 0.0f);
            gl.glVertex2f(x1, y1);
            mainTexCoord.setValue(gl, 0.0f, 0.0f);
            gl.glVertex2f(x0, y1);
            gl.glEnd();
            gl.glColorMask(true, true, true, true);
        }
    }

    private static int genT(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        return tmp[0];
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
    public static void renderImageDataToScreen(GL2 gl, Region region, ImageData source, JHVJP2View jpxView) {
        if (source == null)
            return;
        if (source.getWidth() <= maxTextureSize && source.getHeight() <= maxTextureSize) {
            if (jpxView != null) {
                if (jpxView.texID == -1) {
                    /* to be encapsulated */
                    jpxView.texID = genT(gl);
                    jpxView.gl = gl;
                }
                moveImageDataToGLTexture(gl, source, jpxView.texID);
            }
            renderTextureToScreen(gl, region);

        } else {
            ColorMask colorMask = source.getColorMask();
            if (colorMask.getMask() != 0xFFFFFFFF) {
                gl.glColorMask(colorMask.showRed(), colorMask.showGreen(), colorMask.showBlue(), true);
            }

            Vector2dDouble lowerleftCorner = region.getLowerLeftCorner();
            Vector2dDouble size = region.getSize();

            for (int x = 0; x < source.getWidth(); x += maxTextureSize) {
                for (int y = 0; y < source.getHeight(); y += maxTextureSize) {

                    int width = Math.min(source.getWidth() - x, maxTextureSize);
                    int height = Math.min(source.getHeight() - y, maxTextureSize);
                    if (jpxView != null) {
                        if (jpxView.texID == -1) {
                            jpxView.texID = genT(gl);
                            jpxView.gl = gl;
                        }
                        moveImageDataToGLTexture(gl, source, x, y, width, height, jpxView.texID);
                    }
                    float x0 = (float) lowerleftCorner.getX() + (float) size.getX() * x / source.getWidth();
                    float y1 = (float) lowerleftCorner.getY() + (float) size.getY() * (source.getHeight() - y) / source.getHeight();
                    renderTextureToScreen(gl, x0, y1 - ((float) size.getY()) * (height + 1.5f) / source.getHeight(), x0 + ((float) size.getX()) * (width + 1.5f) / source.getWidth(), y1);
                }
            }
            gl.glColorMask(true, true, true, true);
        }
    }


    /**
     * Saves a given image data object to a given texture.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @param source
     *            Image data to copy to the texture
     * @param target
     *            Valid texture id
     */
    public static void moveImageDataToGLTexture(GL2 gl, ImageData source, int target) {
        moveImageDataToGLTexture(gl, source, 0, 0, source.getWidth(), source.getHeight(), target);
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
    public static void moveImageDataToGLTexture(GL2 gl, ImageData source, int x, int y, int width, int height, int target) {

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

        if (source.getHeight() > 1) {
            textureImplementation.genTexture2D(gl, target, mapImageFormatToInternalGLFormat(imageFormat), width, height, mapImageFormatToInputGLFormat(imageFormat), mapBitsPerPixelToGLType(bitsPerPixel), buffer);
        } else
            throw new IllegalArgumentException("Format is not supported");
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
    public static void moveBufferedImageToGLTexture(GL2 gl, BufferedImage source, int target) {

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

        if (source.getHeight() > 1) {
            textureImplementation.genTexture2D(gl, target, mapTypeToInternalGLFormat(source.getType()), source.getWidth(), source.getHeight(), mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(rawBuffer.getDataType()), buffer);
        } else
            throw new IllegalArgumentException("Format is not supported");
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

    /**
     * Private interface to perform the actual transfer of input data to the
     * graphics card and bind textures
     *
     * <p>
     * This interface has been introduced for not having to check every render
     * cycle, whether non power of two textures are activated or not. There is
     * one implementation per type: {@link NonPowerOfTwoTextureImplementation}
     * and {@link PowerOfTwoTextureImplementation}.
     *
     */
    private static interface TextureImplementation {

        /**
         * Copies image data to a two-dimensional texture to the graphics
         * memory.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param texID
         *            Target texture id
         * @param internalFormat
         *            OpenGL format used for saving the data
         * @param width
         *            Width of the image
         * @param height
         *            Height of the image
         * @param inputFormat
         *            OpenGL format used for transferring the data
         * @param inputType
         *            OpenGL type used for reading the data
         * @param buffer
         *            Source data
         */
        public void genTexture2D(GL2 gl, int texID, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer);

        /**
         * Binds a 2D texture.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param texture
         *            texture id to bind
         * @see #bindTexture(GL2, int, int)
         */
        public void bindTexture(GL2 gl, int texture);

        /**
         * Binds a texture.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param target
         *            texture type, such as GL_TEXTURE_2D
         * @param texture
         *            texture id to bind
         * @see #bindTexture(GL2, int, int)
         */
        public void bindTexture(GL2 gl, int target, int texture);

        /**
         * Copies the current frame buffer to a texture.
         *
         * @param gl
         *            Valid reference to the current gl object
         * @param texture
         *            target texture
         * @param rect
         *            area of the frame buffer to copy
         */
        public void copyFrameBufferToTexture(GL2 gl, int texture, Rectangle rect);
    }

    /**
     * Implementation of Texture Loader for non power of two textures.
     */
    private static class NonPowerOfTwoTextureImplementation implements TextureImplementation {

        /**
         * {@inheritDoc}
         */
        @Override
        public void genTexture2D(GL2 gl, int texID, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {

            gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, buffer);

            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bindTexture(GL2 gl, int texture) {
            gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bindTexture(GL2 gl, int target, int texture) {
            gl.glBindTexture(target, texture);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void copyFrameBufferToTexture(GL2 gl, int texture, Rectangle rect) {
            gl.glCopyTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, rect.x, rect.y, rect.width, rect.height, 0);
        }

    }

    /**
     * Texture implementation for power of two textures. After copying the
     * image, the loader also sets the scaling factor used by
     * {@link GLScalePowerOfTwoView} and
     * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
     * .
     */
    private static class PowerOfTwoTextureImplementation implements TextureImplementation {

        /**
         * {@inheritDoc}
         */
        private int bytebufferlength;

        /**
         * {@inheritDoc}
         */
        @Override
        public void genTexture2D(GL2 gl, int texID, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
            synchronized (allTextures) {
                gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
                int width2 = nextPowerOfTwo(width);
                int height2 = nextPowerOfTwo(height);

                int bpp = 3;
                switch (inputFormat) {
                case GL2.GL_LUMINANCE:
                case GL2.GL_ALPHA:
                    bpp = 1;
                    break;
                case GL2.GL_LUMINANCE_ALPHA:
                    bpp = 2;
                    break;
                case GL2.GL_RGB:
                    bpp = 3;
                    break;
                case GL2.GL_RGBA:
                    bpp = 4;
                    break;
                }
                switch (inputType) {
                case GL2.GL_UNSIGNED_BYTE:
                    bpp *= 1;
                    break;
                case GL2.GL_UNSIGNED_SHORT:
                case GL2.GL_UNSIGNED_SHORT_5_6_5:
                case GL2.GL_UNSIGNED_SHORT_4_4_4_4:
                case GL2.GL_UNSIGNED_SHORT_5_5_5_1:
                    bpp *= 2;
                    break;
                }

                gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width2, height2, 0, inputFormat, inputType, null);

                // Log.debug("GLTextureHelper.genTexture2D: Width="+width+", Height="+height+" Width2="+width2+", Height2="+height2);
                if (buffer != null) {
                    // System.out.println("TEXSUBIM" + width + " " + height);
                    gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, width, height, inputFormat, inputType, buffer);
                }

                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
                //Does the 2D mode need GL_NEAREST??
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);

                scaleX = (float) 1.0 * width / width2;
                scaleY = (float) 1.0 * height / height2;
                scaleTexCoord.setValue(gl, (float) scaleX, (float) scaleY);
                // System.out.println("SCALESET " +scaleX + " " +scaleY);
                allTextures.put(texID, new Vector2dDouble(scaleX, scaleY));
            }
        }

        /**
         * {@inheritDoc}
         *
         * In this case, this function will also load the texture scaling to the
         * texture coordinate GL_TEXTURE1, which will be used by
         * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
         */
        @Override
        public void bindTexture(GL2 gl, int texture) {
            bindTexture(gl, GL2.GL_TEXTURE_2D, texture);
        }

        /**
         * {@inheritDoc}
         *
         * In this case, this function will also load the texture scaling to the
         * texture coordinate GL_TEXTURE1, which will be used by
         * {@link org.helioviewer.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram}
         */
        @Override
        public void bindTexture(GL2 gl, int target, int texture) {
            synchronized (allTextures) {
                gl.glBindTexture(target, texture);

                Vector2dDouble scaleVector = allTextures.get(texture);
                if (scaleVector != null) {
                    scaleTexCoord.setValue(gl, (float) scaleVector.getX(), (float) scaleVector.getY());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void copyFrameBufferToTexture(GL2 gl, int texture, Rectangle rect) {
            int width = nextPowerOfTwo(rect.width);
            int height = nextPowerOfTwo(rect.height);
            // Log.debug("GLTextureHelper.glCopyTexImage2D: Width="+width+", Height="+height+" x="+rect.x+", y="+rect.y);
            gl.glCopyTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, rect.x, rect.y, width, height, 0);
        }

        /**
         * Internal function for calculation the next power of two.
         *
         * The returned value is greater or equal than the input and a power of
         * two.
         *
         * @param input
         *            Search next power of two for this input
         * @return Next power of two
         */
        private int nextPowerOfTwo(int input) {
            int output = 1;
            while (output < input) {
                output <<= 1;
            }
            return output;
        }
    }

    /**
     * GLTextureCoordinate implementation for the standard texture coordinate.
     *
     * This coordinate should be used instead of all calls to gl.glTexCoord2x.
     */
    public static class GLMainTextureCoordinate extends GLTextureCoordinate {

        /**
         * Default constructor
         */
        public GLMainTextureCoordinate() {
            super(GL2.GL_TEXTURE0, 0, 2, "texcoord0.xy");
        }
    }

    /**
     * GLTextureCoordinate implementation for the scaling texture coordinate.
     */
    private static class GLScaleTextureCoordinate extends GLTextureCoordinate {

        /**
         * Default constructor
         */
        protected GLScaleTextureCoordinate() {
            super(GL2.GL_TEXTURE0, 2, 2, "texcoord0.zw");
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
