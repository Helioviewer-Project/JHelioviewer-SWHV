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

import java.awt.Dimension;
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

  // Whether we have an alpha channel in the (RGB/A) backing store
  private final boolean alpha;

  // Whether we're attempting to use automatic mipmap generation support
  private boolean mipmap;

  // Whether smoothing is enabled for the OpenGL texture (switching
  // between GL_LINEAR and GL_NEAREST filtering)
  private boolean smoothing = true;
  private boolean smoothingChanged;

  // The backing store itself
  private BufferedImage image;

  private Texture texture;
  private AWTTextureData textureData;
  private boolean mustReallocateTexture;
  private Rectangle dirtyRegion;

  /** Creates a new renderer with backing store of the specified width
      and height. If <CODE>alpha</CODE> is true, allocates an alpha channel in the
      backing store image. If <CODE>mipmap</CODE> is true, attempts to use OpenGL's
      automatic mipmap generation for better smoothing when rendering
      the TextureRenderer's contents at a distance.

      @param width the width of the texture to render into
      @param height the height of the texture to render into
      @param alpha whether to allocate an alpha channel for the texture
      @param mipmap whether to attempt use of automatic mipmap generation
  */
  JhvTextureRenderer(final int width, final int height, final boolean alpha, final boolean mipmap) {
    this.alpha = alpha;
    this.mipmap = mipmap;
    init(width, height);
  }

  /** Returns the width of the backing store of this renderer.

      @return the width of the backing store of this renderer
  */
  public int getWidth() {
    return image.getWidth();
  }

  /** Returns the height of the backing store of this renderer.

      @return the height of the backing store of this renderer
  */
  public int getHeight() {
    return image.getHeight();
  }

  /** Returns the size of the backing store of this renderer. Uses the
      {@link java.awt.Dimension Dimension} object if one is supplied,
      or allocates a new one if null is passed.

      @param d a {@link java.awt.Dimension Dimension} object in which
        to store the results, or null to allocate a new one

      @return the size of the backing store of this renderer
  */
  private Dimension getSize(Dimension d) {
    if (d == null)
      d = new Dimension();
    d.setSize(image.getWidth(), image.getHeight());
    return d;
  }

  /** Sets the size of the backing store of this renderer. This may
      cause the OpenGL texture object associated with this renderer to
      be invalidated; it is not recommended to cache this texture
      object outside this class but to instead call {@link #getTexture
      getTexture} when it is needed.

      @param width the new width of the backing store of this renderer
      @param height the new height of the backing store of this renderer
      @throws GLException If an OpenGL context is not current when this method is called
  */
  private void setSize(final int width, final int height) throws GLException {
    init(width, height);
  }

  /** Sets the size of the backing store of this renderer. This may
      cause the OpenGL texture object associated with this renderer to
      be invalidated.

      @param d the new size of the backing store of this renderer
      @throws GLException If an OpenGL context is not current when this method is called
  */
  public void setSize(final Dimension d) throws GLException {
    setSize(d.width, d.height);
  }

  /** Sets whether smoothing is enabled for the OpenGL texture; if so,
      uses GL_LINEAR interpolation for the minification and
      magnification filters. Defaults to true. Changes to this setting
      will not take effect until the next call to {@link
      #beginOrthoRendering beginOrthoRendering}.

      @param smoothing whether smoothing is enabled for the OpenGL texture
  */
  public void setSmoothing(final boolean smoothing) {
    this.smoothing = smoothing;
    smoothingChanged = true;
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

  /** Indicates whether automatic mipmap generation is in use for this
      TextureRenderer. The result of this method may change from true
      to false if it is discovered during allocation of the
      TextureRenderer's backing store that automatic mipmap generation
      is not supported at the OpenGL level. */
  public boolean isUsingAutoMipmapGeneration() {
    return mipmap;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void beginRendering(final boolean ortho, final int width, final int height) {
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    if (ortho) {
      gl.glDisable(GL.GL_DEPTH_TEST);

      Transform.pushProjection();
      Transform.setOrthoProjection(0, width, 0, height, -1, 1);
      Transform.pushView();
      Transform.setIdentityView();
    }

    final Texture texture = getTexture();
    texture.enable(gl);
    texture.bind(gl);

    if (smoothingChanged) {
      smoothingChanged = false;
      if (smoothing) {
        texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        if (mipmap) {
          texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        } else {
          texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }
      } else {
        texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      }
    }
  }

  private void endRendering(final boolean ortho) {
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    final Texture texture = getTexture();
    texture.disable(gl);
    if (ortho) {
      gl.glEnable(GL.GL_DEPTH_TEST);

      Transform.popView();
      Transform.popProjection();
    }
  }

  private void init(final int width, final int height) {
    final GL2 gl = (GL2) GLContext.getCurrentGL();
    // Discard previous BufferedImage if any
    if (image != null) {
      image.flush();
      image = null;
    }

    final int internalFormat = GL2.GL_RGBA; // force for high version OpenGL
    final int imageType = (alpha ?  BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_RGB);
    image = new BufferedImage(width, height, imageType);
    // Always realllocate the TextureData associated with this
    // BufferedImage; it's just a reference to the contents but we
    // need it in order to update sub-regions of the underlying
    // texture
    textureData = new AWTTextureData(gl.getGLProfile(), internalFormat, 0, mipmap, image);
    // For now, always reallocate the underlying OpenGL texture when
    // the backing store size changes
    mustReallocateTexture = true;
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
      texture.updateSubImage(GLContext.getCurrentGL(), textureData, 0, x, y, x, y, width, height);
    }
  }

  // Returns true if the texture was newly allocated, false if not
  private boolean ensureTexture() {
    final GL gl = GLContext.getCurrentGL();
    if (mustReallocateTexture) {
      if (texture != null) {
        texture.destroy(gl);
        texture = null;
      }
      mustReallocateTexture = false;
    }

    if (texture == null) {
      texture = TextureIO.newTexture(textureData);
      if (mipmap && !texture.isUsingAutoMipmapGeneration()) {
        // Only try this once
        texture.destroy(gl);
        mipmap = false;
        textureData.setMipmap(false);
        texture = TextureIO.newTexture(textureData);
      }

      if (!smoothing) {
        // The TextureIO classes default to GL_LINEAR filtering
        texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      }
      return true;
    }
    return false;
  }

}
