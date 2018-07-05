/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package org.helioviewer.jhv.opengl.text;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.*;
import java.nio.IntBuffer;

import com.jogamp.opengl.*;

import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.GLTexture;

/**
 * Provides the ability to render into an OpenGL {@link
 * com.jogamp.opengl.util.texture.Texture Texture} using the Java 2D
 * APIs. This renderer class uses an internal Java 2D image (of
 * unspecified type) for its backing store and flushes portions of
 * that image to an OpenGL texture on demand. The resulting OpenGL
 * texture can then be mapped on to a polygon for display.
 */

class JhvTextureRenderer {
    // For now, we supply only a BufferedImage back-end for this
    // renderer. In theory we could use the Java 2D/JOGL bridge to fully
    // accelerate the rendering paths, but there are restrictions on
    // what work can be done where; for example, Graphics2D-related work
    // must not be done on the Queue Flusher Thread, but JOGL's
    // OpenGL-related work must be. This implies that the user's code
    // would need to be split up into multiple callbacks run from the
    // appropriate threads, which would be somewhat unfortunate.

    // The backing store itself
    private BufferedImage image;
    private IntBuffer imageBuffer;

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

        image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        imageBuffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());

        GL2 gl = (GL2) GLContext.getCurrentGL();
        tex = new GLTexture(gl);
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 15);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, imageWidth, imageHeight, 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, null);
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

    void bind(GL2 gl) throws GLException {
        tex.bind(gl, GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE0);
        if (dirtyRegion != null) {
            upload(gl, dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
            dirtyRegion = null;
        }
    }

    /**
     * Disposes all resources associated with this renderer. It is not
     * valid to use this renderer after calling this method.
     *
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    void dispose() throws GLException {
        tex.delete((GL2) GLContext.getCurrentGL());
        imageBuffer = null;
        image = null;
    }

    static void beginRendering(boolean ortho, int width, int height) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glDisable(GL2.GL_DEPTH_TEST);

            Transform.pushProjection();
            Transform.setOrthoProjection(0, width, 0, height, -1, 1);
            Transform.pushView();
            Transform.setIdentityView();
        }
    }

    static void endRendering(boolean ortho) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glEnable(GL2.GL_DEPTH_TEST);

            Transform.popView();
            Transform.popProjection();
        }
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
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    private void upload(GL2 gl, int x, int y, int width, int height) throws GLException {
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, imageWidth);
        gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, x, y, width, height, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, imageBuffer.position(y * imageWidth + x));
        gl.glGenerateMipmap(GL2.GL_TEXTURE_2D);
    }

}
