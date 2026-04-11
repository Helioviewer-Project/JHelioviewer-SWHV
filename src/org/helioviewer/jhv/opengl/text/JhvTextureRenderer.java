package org.helioviewer.jhv.opengl.text;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.imagedata.nio.NativeImageFactory;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLTexture;

/**
 * Provides the ability to render into an OpenGL texture using the Java 2D
 * APIs. This renderer class uses an internal Java 2D image (of
 * unspecified type) for its backing store and flushes portions of
 * that image to an OpenGL texture on demand. The resulting OpenGL
 * texture can then be mapped on to a polygon for display.
 */

class JhvTextureRenderer {
    // For now, we supply only a BufferedImage back-end for this
    // renderer. In theory we could use a Java 2D / OpenGL bridge to fully
    // accelerate the rendering paths, but there are restrictions on
    // what work can be done where; for example, Graphics2D-related work
    // must not be done on the Queue Flusher Thread, while
    // OpenGL-related work must. This implies that the user's code
    // would need to be split up into multiple callbacks run from the
    // appropriate threads, which would be somewhat unfortunate.

    // The backing store itself
    private BufferedImage image;
    private ByteBuffer imageBuffer;

    private final GLTexture tex;
    private Rectangle dirtyRegion;

    private final int imageWidth;
    private final int imageHeight;

    /**
     * Creates a new renderer with backing store of the specified width
     * and height.
     *
     * @param width  the width of the texture to render into
     * @param height the height of the texture to render into
     */
    JhvTextureRenderer(int width, int height) {
        imageWidth = width;
        imageHeight = height;

        image = NativeImageFactory.createRGBAPremultipliedImage(imageWidth, imageHeight);
        imageBuffer = NativeImageFactory.getByteBuffer(image);

        tex = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        tex.bind();
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_BASE_LEVEL, 0);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 15);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
        GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
        GL.glTexImage2D(GL.TEXTURE_2D, 0, GL.RGBA, imageWidth, imageHeight, 0, GL.RGBA, GL.UNSIGNED_BYTE, (ByteBuffer) null);
    }

    int getWidth() {
        return imageWidth;
    }

    int getHeight() {
        return imageHeight;
    }

    /**
     * Creates a {@link java.awt.Graphics2D Graphics2D} instance for
     * rendering to the backing store of this renderer. The returned
     * object should be disposed of using the normal {@link
     * java.awt.Graphics#dispose() Graphics.dispose()} method once it
     * is no longer being used.
     *
     * @return a new {@link java.awt.Graphics2D Graphics2D} object for
     * rendering into the backing store of this renderer
     */
    Graphics2D createGraphics() {
        return image.createGraphics();
    }

    /**
     * Returns the underlying Java 2D {@link java.awt.Image Image}
     * being rendered into.
     */
    Image getImage() {
        return image;
    }

    /**
     * Marks the given region of the TextureRenderer as dirty. This
     * region, and any previously set dirty regions, will be
     * automatically synchronized with the underlying Texture during
     * the next bind operation, at which
     * point the dirty region will be cleared. It is not necessary for
     * an OpenGL context to be current when this method is called.
     *
     * @param x      the x coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param y      the y coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param width  the width of the region to update
     * @param height the height of the region to update
     */
    void markDirty(int x, int y, int width, int height) {
        Rectangle curRegion = new Rectangle(x, y, width, height);
        if (dirtyRegion == null) {
            dirtyRegion = curRegion;
        } else {
            dirtyRegion.add(curRegion);
        }
    }

    void bind() {
        tex.bind();
        if (dirtyRegion != null) {
            upload(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
            dirtyRegion = null;
        }
    }

    /**
     * Disposes all resources associated with this renderer. It is not
     * valid to use this renderer after calling this method.
     */
    void dispose() {
        tex.delete();
        imageBuffer = null;
        NativeImageFactory.free(image);
        image = null;
    }

    /**
     * Synchronizes the specified region of the backing store down to
     * the underlying OpenGL texture. If {@link #markDirty markDirty}
     * is used instead to indicate the regions that are out of sync,
     * this method does not need to be called.
     *
     * @param x      the x coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param y      the y coordinate (in Java 2D coordinates -- relative to
     *               upper left) of the region to update
     * @param width  the width of the region to update
     * @param height the height of the region to update
     */
    private void upload(int x, int y, int width, int height) {
        GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 4);
        GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, imageWidth);
        GL.glTexSubImage2D(GL.TEXTURE_2D, 0, x, y, width, height, GL.RGBA, GL.UNSIGNED_BYTE, imageBuffer.position(4 * (y * imageWidth + x)));
        GL.glGenerateMipmap(GL.TEXTURE_2D);
        imageBuffer.rewind();
    }

}
