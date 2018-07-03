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

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;

import org.helioviewer.jhv.math.Transform;

/** Provides the ability to render into an OpenGL {@link
    com.jogamp.opengl.util.texture.Texture Texture} using the Java 2D
    APIs. This renderer class uses an internal Java 2D image (of
    unspecified type) for its backing store and flushes portions of
    that image to an OpenGL texture on demand. The resulting OpenGL
    texture can then be mapped on to a polygon for display. */

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

  private Texture texture;
  private final AWTTextureData textureData;
  private boolean mustReallocateTexture;
  private Rectangle dirtyRegion;

  private final int width;
  private final int height;

  /** Creates a new renderer with backing store of the specified width
      and height.
      @param width the width of the texture to render into
      @param height the height of the texture to render into
  */
  JhvTextureRenderer(int _width, int _height) {
    width = _width;
    height = _height;
    int internalFormat = GL2.GL_RGBA; // force for high version OpenGL
    int imageType = BufferedImage.TYPE_INT_ARGB_PRE;
    image = new BufferedImage(width, height, imageType);
    // Always reallocate the TextureData associated with this
    // BufferedImage; it's just a reference to the contents but we
    // need it in order to update sub-regions of the underlying
    // texture
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    textureData = new AWTTextureData(gl.getGLProfile(), internalFormat, 0, true, image);
    // For now, always reallocate the underlying OpenGL texture when
    // the backing store size changes
    mustReallocateTexture = true;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /** Creates a {@link java.awt.Graphics2D Graphics2D} instance for
      rendering to the backing store of this renderer. The returned
      object should be disposed of using the normal {@link
      java.awt.Graphics#dispose() Graphics.dispose()} method once it
      is no longer being used.

      @return a new {@link java.awt.Graphics2D Graphics2D} object for
        rendering into the backing store of this renderer
  */
  public Graphics2D createGraphics() {
    return image.createGraphics();
  }

  /** Returns the underlying Java 2D {@link java.awt.Image Image}
      being rendered into. */
  public Image getImage() {
    return image;
  }

  /** Marks the given region of the TextureRenderer as dirty. This
      region, and any previously set dirty regions, will be
      automatically synchronized with the underlying Texture during
      the next {@link #getTexture getTexture} operation, at which
      point the dirty region will be cleared. It is not necessary for
      an OpenGL context to be current when this method is called.

      @param x the x coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param y the y coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param width the width of the region to update
      @param height the height of the region to update
  */
  public void markDirty(final int x, final int y, final int width, final int height) {
    final Rectangle curRegion = new Rectangle(x, y, width, height);
    if (dirtyRegion == null) {
      dirtyRegion = curRegion;
    } else {
      dirtyRegion.add(curRegion);
    }
  }

  /** Returns the underlying OpenGL Texture object associated with
      this renderer, synchronizing any dirty regions of the
      TextureRenderer with the underlying OpenGL texture.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public Texture getTexture() throws GLException {
    if (dirtyRegion != null) {
      sync(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
      dirtyRegion = null;
    }

    ensureTexture();
    return texture;
  }

  /** Disposes all resources associated with this renderer. It is not
      valid to use this renderer after calling this method.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void dispose() throws GLException {
    if (texture != null) {
      texture.destroy(GLContext.getCurrentGL());
      texture = null;
    }
    if (image != null) {
      image.flush();
      image = null;
    }
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen, if the application intends to draw
      them as a flat overlay on to the screen. Pushes OpenGL state
      bits (GL_ENABLE_BIT, GL_DEPTH_BUFFER_BIT and GL_TRANSFORM_BIT);
      disables the depth test, back-face culling, and lighting;
      enables the texture in this renderer; and sets up the viewing
      matrices for orthographic rendering where the coordinates go
      from (0, 0) at the lower left to (width, height) at the upper
      right. Equivalent to beginOrthoRendering(width, height, true).
      {@link #endOrthoRendering} must be used in conjunction with this
      method to restore all OpenGL states.

      @param width the width of the current on-screen OpenGL drawable
      @param height the height of the current on-screen OpenGL drawable

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void beginOrthoRendering(final int width, final int height) throws GLException {
    beginRendering(true, width, height);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen as 2D quads in 3D space. Pushes
      OpenGL state (GL_ENABLE_BIT); disables lighting; and enables the
      texture in this renderer. Unlike {@link #beginOrthoRendering
      beginOrthoRendering}, does not modify the depth test, back-face
      culling, lighting, or the modelview or projection matrices. {@link
      #end3DRendering} must be used in conjunction with this method to
      restore all OpenGL states.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void begin3DRendering() throws GLException {
    beginRendering(false, 0, 0);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen, if the application intends to draw
      them as a flat overlay on to the screen. Must be used if {@link
      #beginOrthoRendering} is used to set up the rendering stage for
      this overlay.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void endOrthoRendering() throws GLException {
    endRendering(true);
  }

  /** Convenience method which assists in rendering portions of the
      OpenGL texture to the screen as 2D quads in 3D space. Must be
      used if {@link #begin3DRendering} is used to set up the
      rendering stage for this overlay.

      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void end3DRendering() throws GLException {
    endRendering(false);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void beginRendering(final boolean ortho, final int width, final int height) {
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    if (ortho) {
      gl.glDisable(GL2.GL_DEPTH_TEST);

      Transform.pushProjection();
      Transform.setOrthoProjection(0, width, 0, height, -1, 1);
      Transform.pushView();
      Transform.setIdentityView();
    }

    getTexture().bind(gl);
  }

  private void endRendering(final boolean ortho) {
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    if (ortho) {
      gl.glEnable(GL2.GL_DEPTH_TEST);

      Transform.popView();
      Transform.popProjection();
    }
  }

  /** Synchronizes the specified region of the backing store down to
      the underlying OpenGL texture. If {@link #markDirty markDirty}
      is used instead to indicate the regions that are out of sync,
      this method does not need to be called.

      @param x the x coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param y the y coordinate (in Java 2D coordinates -- relative to
        upper left) of the region to update
      @param width the width of the region to update
      @param height the height of the region to update

      @throws GLException If an OpenGL context is not current when this method is called
  */
  private void sync(final int x, final int y, final int width, final int height) throws GLException {
    // Force allocation if necessary
    final boolean canSkipUpdate = ensureTexture();

    if (!canSkipUpdate) {
      // Update specified region.
      // NOTE that because BufferedImage-based TextureDatas now don't
      // do anything to their contents, the coordinate systems for
      // OpenGL and Java 2D actually line up correctly for
      // updateSubImage calls, so we don't need to do any argument
      // conversion here (i.e., flipping the Y coordinate).
      final GL2 gl = (GL2) GLContext.getCurrentGL();
      texture.updateSubImage(gl, textureData, 0, x, y, x, y, width, height);
      gl.glGenerateMipmap(GL2.GL_TEXTURE_2D);
    }
  }

  // Returns true if the texture was newly allocated, false if not
  private boolean ensureTexture() {
    if (mustReallocateTexture) {
      final GL2 gl = (GL2) GLContext.getCurrentGL();
      if (texture != null) {
        texture.destroy(gl);
        texture = null;
      }
      mustReallocateTexture = false;
    }

    if (texture == null) {
      final GL2 gl = (GL2) GLContext.getCurrentGL();
      texture = TextureIO.newTexture(textureData);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_BASE_LEVEL, 0);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAX_LEVEL, 15);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
      texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
      return true;
    }
    return false;
  }

}
