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

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.packrect.*;

import java.awt.AlphaComposite;
import java.awt.Color;

// For debugging purposes
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.*;
import java.awt.geom.*;
import java.nio.*;
import java.text.*;
import java.util.*;

import com.jogamp.opengl.*;
//import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLSLTexture;

/**
 * Renders bitmapped Java 2D text into an OpenGL window with high
 * performance, full Unicode support, and a simple API. Performs
 * appropriate caching of text rendering results in an OpenGL texture
 * internally to avoid repeated font rasterization. The caching is
 * completely automatic, does not require any user intervention, and
 * has no visible controls in the public API. <P>
 * <p>
 * Using the {@link JhvTextRenderer TextRenderer} is simple. Add a
 * "<code>TextRenderer renderer;</code>" field to your {@link
 * com.jogamp.opengl.GLEventListener GLEventListener}. In your {@link
 * com.jogamp.opengl.GLEventListener#init init} method, add:
 *
 * <PRE>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </PRE>
 *
 * <P> In the {@link com.jogamp.opengl.GLEventListener#display display} method of your
 * {@link com.jogamp.opengl.GLEventListener GLEventListener}, add:
 * <PRE>
 * renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
 * // optionally set the color
 * renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
 * renderer.draw("Text to draw", xPosition, yPosition);
 * // ... more draw commands, color changes, etc.
 * renderer.endRendering();
 * </PRE>
 * <p>
 * Unless you are sharing textures and display lists between OpenGL
 * contexts, you do not need to call the {@link #dispose dispose}
 * method of the TextRenderer; the OpenGL resources it uses
 * internally will be cleaned up automatically when the OpenGL
 * context is destroyed. <P>
 *
 * <b>Note</b> that the TextRenderer may cause the vertex and texture
 * coordinate array buffer bindings to change, or to be unbound. This
 * is important to note if you are using Vertex Buffer Objects (VBOs)
 * in your application. <P>
 * <p>
 * Internally, the renderer uses a rectangle packing algorithm to
 * pack both glyphs and full Strings' rendering results (which are
 * variable size) onto a larger OpenGL texture. The internal backing
 * store is maintained using a {@link
 * com.jogamp.opengl.util.awt.TextureRenderer TextureRenderer}. A least
 * recently used (LRU) algorithm is used to discard previously
 * rendered strings; the specific algorithm is undefined, but is
 * currently implemented by flushing unused Strings' rendering
 * results every few hundred rendering cycles, where a rendering
 * cycle is defined as a pair of calls to {@link #beginRendering
 * beginRendering} / {@link #endRendering endRendering}.
 *
 * @author John Burkey
 * @author Kenneth Russell
 */
public class JhvTextRenderer {

    private static final boolean DRAW_BBOXES = false;

    private static final int kSize = 256;

    // Every certain number of render cycles, flush the strings which
    // haven't been used recently
    private static final int CYCLES_PER_FLUSH = 100;

    // The amount of vertical dead space on the backing store before we
    // force a compaction
    private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;
    private static final int kQuadsPerBuffer = 100;
    private static final int kCoordsPerVertVerts = 4;
    private static final int kCoordsPerVertTex = 2;
    private static final int kVertsPerQuad = 6;
    private static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
    private static final int kTotalBufferSizeCoordsVerts = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertVerts;
    private static final int kTotalBufferSizeCoordsTex = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertTex;
    final Font font;
    private final boolean antialiased;
    private final boolean useFractionalMetrics;

    RectanglePacker packer;
    private boolean haveMaxSize;
    final RenderDelegate renderDelegate;
    private JhvTextureRenderer cachedBackingStore;
    private Graphics2D cachedGraphics;
    private FontRenderContext cachedFontRenderContext;
    final Map<String, Rect> stringLocations = new HashMap<>();
    final GlyphProducer mGlyphProducer;

    private int numRenderCycles;

    // Need to keep track of whether we're in a beginRendering() /
    // endRendering() cycle so we can re-enter the exact same state if
    // we have to reallocate the backing store
    boolean inBeginEndPair;
    boolean isOrthoMode;
    int beginRenderingWidth;
    int beginRenderingHeight;

    Pipelined_QuadRenderer mPipelinedQuadRenderer;

    /**
     * Creates a new TextRenderer with the given Font, specified font
     * properties, and given RenderDelegate. The
     * <code>antialiased</code> and <code>useFractionalMetrics</code>
     * flags provide control over the same properties at the Java 2D
     * level. The <code>renderDelegate</code> provides more control
     * over the text rendered.
     *
     * @param font                 the font to render with
     * @param antialiased          whether to use antialiased fonts
     * @param useFractionalMetrics whether to use fractional font
     *                             metrics at the Java 2D level
     * @param renderDelegate       the render delegate to use to draw the
     *                             text's bitmap, or null to use the default one
     */
    public JhvTextRenderer(final Font font, final boolean antialiased,
                           final boolean useFractionalMetrics, RenderDelegate renderDelegate) {
        this.font = font;
        this.antialiased = antialiased;
        this.useFractionalMetrics = useFractionalMetrics;

        // FIXME: consider adjusting the size based on font size
        // (it will already automatically resize if necessary)
        packer = new RectanglePacker(new Manager(), kSize, kSize);

        if (renderDelegate == null) {
            renderDelegate = new DefaultRenderDelegate();
        }

        this.renderDelegate = renderDelegate;

        mGlyphProducer = new GlyphProducer(font.getNumGlyphs());
    }

    /**
     * Returns the bounding rectangle of the given String, assuming it
     * was rendered at the origin. See {@link #getBounds(CharSequence)
     * getBounds(CharSequence)}.
     */
    public Rectangle2D getBounds(final String str) {
        return getBounds((CharSequence) str);
    }

    /**
     * Returns the bounding rectangle of the given CharSequence,
     * assuming it was rendered at the origin. The coordinate system of
     * the returned rectangle is Java 2D's, with increasing Y
     * coordinates in the downward direction. The relative coordinate
     * (0, 0) in the returned rectangle corresponds to the baseline of
     * the leftmost character of the rendered string, in similar
     * fashion to the results returned by, for example, {@link
     * java.awt.font.GlyphVector#getVisualBounds}. Most applications
     * will use only the width and height of the returned Rectangle for
     * the purposes of centering or justifying the String. It is not
     * specified which Java 2D bounds ({@link
     * java.awt.font.GlyphVector#getVisualBounds getVisualBounds},
     * {@link java.awt.font.GlyphVector#getPixelBounds getPixelBounds},
     * etc.) the returned bounds correspond to, although every effort
     * is made to ensure an accurate bound.
     */
    private Rectangle2D getBounds(final CharSequence str) {
        // FIXME: this should be more optimized and use the glyph cache
        final Rect r = stringLocations.get(str);

        if (r != null) {
            final TextData data = (TextData) r.getUserData();

            // Reconstitute the Java 2D results based on the cached values
            return new Rectangle2D.Double(-data.origin().x, -data.origin().y,
                    r.w(), r.h());
        }

        // Must return a Rectangle compatible with the layout algorithm --
        // must be idempotent
        return normalize(renderDelegate.getBounds(str, font,
                getFontRenderContext()));
    }

    /**
     * Returns the Font this renderer is using.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns a FontRenderContext which can be used for external
     * text-related size computations. This object should be considered
     * transient and may become invalidated between {@link
     * #beginRendering beginRendering} / {@link #endRendering
     * endRendering} pairs.
     */
    FontRenderContext getFontRenderContext() {
        if (cachedFontRenderContext == null) {
            cachedFontRenderContext = getGraphics2D().getFontRenderContext();
        }

        return cachedFontRenderContext;
    }

    /**
     * Begins rendering with this {@link JhvTextRenderer TextRenderer}
     * into the current OpenGL drawable, pushing the projection and
     * modelview matrices and some state bits and setting up a
     * two-dimensional orthographic projection with (0, 0) as the
     * lower-left coordinate and (width, height) as the upper-right
     * coordinate. Binds and enables the internal OpenGL texture
     * object, sets the texture environment mode to GL_MODULATE, and
     * changes the current color to the last color set with this
     * TextRenderer via {@link #setColor setColor}. Disables the depth
     * test if the disableDepthTest argument is true.
     *
     * @param width            the width of the current on-screen OpenGL drawable
     * @param height           the height of the current on-screen OpenGL drawable
     * @param disableDepthTest whether to disable the depth test
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void beginRendering(final int width, final int height, final boolean disableDepthTest)
            throws GLException {
        beginRendering(true, width, height);
    }

    /**
     * Begins rendering of 2D text in 3D with this {@link JhvTextRenderer
     * TextRenderer} into the current OpenGL drawable. Assumes the end
     * user is responsible for setting up the modelview and projection
     * matrices, and will render text using the {@link #draw3D draw3D}
     * method. This method pushes some OpenGL state bits, binds and
     * enables the internal OpenGL texture object, sets the texture
     * environment mode to GL_MODULATE, and changes the current color
     * to the last color set with this TextRenderer via {@link
     * #setColor setColor}.
     *
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void begin3DRendering() throws GLException {
        beginRendering(false, 0, 0);
    }

    /**
     * Changes the current color of this TextRenderer to the supplied
     * one, where each component ranges from 0.0f - 1.0f. The alpha
     * component, if used, does not need to be premultiplied into the
     * color channels as described in the documentation for {@link
     * com.jogamp.opengl.util.texture.Texture Texture}, although
     * premultiplied colors are used internally. The default color is
     * opaque white.
     */
    public void setColor(float[] color)
            throws GLException {
        flushGlyphPipeline();
        textColor = color;
    }

    /**
     * Draws the supplied CharSequence at the desired location using
     * the renderer's current color. The baseline of the leftmost
     * character is at position (x, y) specified in OpenGL coordinates,
     * where the origin is at the lower-left of the drawable and the Y
     * coordinate increases in the upward direction.
     *
     * @param str the string to draw
     * @param x   the x coordinate at which to draw
     * @param y   the y coordinate at which to draw
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void draw(final CharSequence str, final int x, final int y) throws GLException {
        draw3D(str, x, y, 0, 1);
    }

    /**
     * Draws the supplied String at the desired location using the
     * renderer's current color. See {@link #draw(CharSequence, int,
     * int) draw(CharSequence, int, int)}.
     */
    public void draw(final String str, final int x, final int y) throws GLException {
        draw3D(str, x, y, 0, 1);
    }

    /**
     * Draws the supplied CharSequence at the desired 3D location using
     * the renderer's current color. The baseline of the leftmost
     * character is placed at position (x, y, z) in the current
     * coordinate system.
     *
     * @param str         the string to draw
     * @param x           the x coordinate at which to draw
     * @param y           the y coordinate at which to draw
     * @param z           the z coordinate at which to draw
     * @param scaleFactor a uniform scale factor applied to the width and height of the drawn rectangle
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    private void draw3D(final CharSequence str, final float x, final float y, final float z,
                        final float scaleFactor) {
        internal_draw3D(str, x, y, z, scaleFactor);
    }

    /**
     * Draws the supplied String at the desired 3D location using the
     * renderer's current color. See {@link #draw3D(CharSequence,
     * float, float, float, float) draw3D(CharSequence, float, float,
     * float, float)}.
     */
    public void draw3D(final String str, final float x, final float y, final float z, final float scaleFactor) {
        internal_draw3D(str, x, y, z, scaleFactor);
    }

    /**
     * Returns the pixel width of the given character.
     */
    public float getCharWidth(final char inChar) {
        return mGlyphProducer.getGlyphPixelWidth(inChar);
    }

    /**
     * Causes the TextRenderer to flush any internal caches it may be
     * maintaining and draw its rendering results to the screen. This
     * should be called after each call to draw() if you are setting
     * OpenGL state such as the modelview matrix between calls to
     * draw().
     */
    public void flush() {
        flushGlyphPipeline();
    }

    /**
     * Ends a render cycle with this {@link JhvTextRenderer TextRenderer}.
     * Restores the projection and modelview matrices as well as
     * several OpenGL state bits. Should be paired with {@link
     * #beginRendering beginRendering}.
     *
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void endRendering() throws GLException {
        endRendering(true);
    }

    /**
     * Ends a 3D render cycle with this {@link JhvTextRenderer TextRenderer}.
     * Restores several OpenGL state bits. Should be paired with {@link
     * #begin3DRendering begin3DRendering}.
     *
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void end3DRendering() throws GLException {
        endRendering(false);
    }

    /**
     * Disposes of all resources this TextRenderer is using. It is not
     * valid to use the TextRenderer after this method is called.
     *
     * @throws GLException If an OpenGL context is not current when this method is called
     */
    public void dispose(GL2 gl) throws GLException {
        packer.dispose();
        packer = null;
        cachedBackingStore = null;
        cachedGraphics = null;
        cachedFontRenderContext = null;
        glslTexture.dispose(gl);
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    static Rectangle2D preNormalize(final Rectangle2D src) {
        // Need to round to integer coordinates
        // Also give ourselves a little slop around the reported
        // bounds of glyphs because it looks like neither the visual
        // nor the pixel bounds works perfectly well
        final int minX = (int) Math.floor(src.getMinX()) - 1;
        final int minY = (int) Math.floor(src.getMinY()) - 1;
        final int maxX = (int) Math.ceil(src.getMaxX()) + 1;
        final int maxY = (int) Math.ceil(src.getMaxY()) + 1;
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    Rectangle2D normalize(final Rectangle2D src) {
        // Give ourselves a boundary around each entity on the backing
        // store in order to prevent bleeding of nearby Strings due to
        // the fact that we use linear filtering

        // NOTE that this boundary is quite heuristic and is related
        // to how far away in 3D we may view the text --
        // heuristically, 1.5% of the font's height
        final int boundary = (int) Math.max(1, 0.015 * font.getSize());

        return new Rectangle2D.Double((int) Math.floor(src.getMinX() - boundary),
                (int) Math.floor(src.getMinY() - boundary),
                (int) Math.ceil(src.getWidth() + 2 * boundary),
                (int) Math.ceil(src.getHeight()) + 2 * boundary);
    }

    JhvTextureRenderer getBackingStore() {
        final JhvTextureRenderer renderer = (JhvTextureRenderer) packer.getBackingStore();

        if (renderer != cachedBackingStore) {
            // Backing store changed since last time; discard any cached Graphics2D
            if (cachedGraphics != null) {
                cachedGraphics.dispose();
                cachedGraphics = null;
                cachedFontRenderContext = null;
            }
            cachedBackingStore = renderer;
        }

        return cachedBackingStore;
    }

    Graphics2D getGraphics2D() {
        final JhvTextureRenderer renderer = getBackingStore();

        if (cachedGraphics == null) {
            cachedGraphics = renderer.createGraphics();
            // Set up composite, font and rendering hints
            cachedGraphics.setComposite(AlphaComposite.Src);
            cachedGraphics.setColor(Color.WHITE);
            cachedGraphics.setFont(font);
            cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                            : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
            cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    (useFractionalMetrics
                            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
        }

        return cachedGraphics;
    }

    private void beginRendering(final boolean ortho, final int width, final int height) {
        final GL2 gl = (GL2) GLContext.getCurrentGL();

        inBeginEndPair = true;
        isOrthoMode = ortho;
        beginRenderingWidth = width;
        beginRenderingHeight = height;

        if (ortho) {
            getBackingStore().beginOrthoRendering(width, height);
        } else {
            getBackingStore().begin3DRendering();
        }

        // Push client attrib bits used by the pipelined quad renderer
        // gl.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);

        if (!haveMaxSize) {
            // Query OpenGL for the maximum texture size and set it in the
            // RectanglePacker to keep it from expanding too large
            final int[] sz = new int[1];
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, sz, 0);
            packer.setMaxSize(sz[0], sz[0]);
            haveMaxSize = true;
        }
    }

    /**
     * emzic: here the call to glBindBuffer crashes on certain graphicscard/driver combinations
     * this is why the ugly try-catch block has been added, which falls back to the old textrenderer
     */
    private void endRendering(final boolean ortho) throws GLException {
        flushGlyphPipeline();

        inBeginEndPair = false;

        if (ortho) {
            getBackingStore().endOrthoRendering();
        } else {
            getBackingStore().end3DRendering();
        }

        if (++numRenderCycles >= CYCLES_PER_FLUSH) {
            numRenderCycles = 0;
            clearUnusedEntries();
        }
    }

    void clearUnusedEntries() {
        final List<Rect> deadRects = new ArrayList<>();

        // Iterate through the contents of the backing store, removing
        // text strings that haven't been used recently
        packer.visit(rect -> {
            final TextData data = (TextData) rect.getUserData();

            if (data.used()) {
                data.clearUsed();
            } else {
                deadRects.add(rect);
            }
        });

        for (final Rect r : deadRects) {
            packer.remove(r);
            stringLocations.remove(((TextData) r.getUserData()).string());

            final int unicodeToClearFromCache = ((TextData) r.getUserData()).unicodeID;

            if (unicodeToClearFromCache > 0) {
                mGlyphProducer.clearCacheEntry(unicodeToClearFromCache);
            }
        }

        // If we removed dead rectangles this cycle, try to do a compaction
        final float frag = packer.verticalFragmentationRatio();

        if (!deadRects.isEmpty() && (frag > MAX_VERTICAL_FRAGMENTATION)) {
            packer.compact();
        }
    }

    private void internal_draw3D(final CharSequence str, float x, final float y, final float z,
                                 final float scaleFactor) {
        for (final Glyph glyph : mGlyphProducer.getGlyphs(str)) {
            final float advance = glyph.draw3D(x, y, z, scaleFactor);
            x += advance * scaleFactor;
        }
    }

    private void flushGlyphPipeline() {
        if (mPipelinedQuadRenderer != null) {
            mPipelinedQuadRenderer.draw();
        }
    }

    /**
     * Class supporting more full control over the process of rendering
     * the bitmapped text. Allows customization of whether the backing
     * store text bitmap is full-color or intensity only, the size of
     * each individual rendered text rectangle, and the contents of
     * each individual rendered text string. The default implementation
     * of this interface uses an intensity-only texture, a
     * closely-cropped rectangle around the text, and renders text
     * using the color white, which is modulated by the set color
     * during the rendering process.
     */
    interface RenderDelegate {
        /**
         * Computes the bounds of the given String relative to the
         * origin.
         */
        Rectangle2D getBounds(String str, Font font,
                              FontRenderContext frc);

        /**
         * Computes the bounds of the given character sequence relative
         * to the origin.
         */
        Rectangle2D getBounds(CharSequence str, Font font,
                              FontRenderContext frc);

        /**
         * Computes the bounds of the given GlyphVector, already
         * assumed to have been created for a particular Font,
         * relative to the origin.
         */
        Rectangle2D getBounds(GlyphVector gv, FontRenderContext frc);

        /**
         * Render the passed character sequence at the designated
         * location using the supplied Graphics2D instance. The
         * surrounding region will already have been cleared to the RGB
         * color (0, 0, 0) with zero alpha. The initial drawing context
         * of the passed Graphics2D will be set to use
         * AlphaComposite.Src, the color white, the Font specified in the
         * TextRenderer's constructor, and the rendering hints specified
         * in the TextRenderer constructor.  Changes made by the end user
         * may be visible in successive calls to this method, but are not
         * guaranteed to be preserved.  Implementors of this method
         * should reset the Graphics2D's state to that desired each time
         * this method is called, in particular those states which are
         * not the defaults.
         */
        void draw(Graphics2D graphics, String str, int x, int y);

        /**
         * Render the passed GlyphVector at the designated location using
         * the supplied Graphics2D instance. The surrounding region will
         * already have been cleared to the RGB color (0, 0, 0) with zero
         * alpha. The initial drawing context of the passed Graphics2D
         * will be set to use AlphaComposite.Src, the color white, the
         * Font specified in the TextRenderer's constructor, and the
         * rendering hints specified in the TextRenderer constructor.
         * Changes made by the end user may be visible in successive
         * calls to this method, but are not guaranteed to be preserved.
         * Implementors of this method should reset the Graphics2D's
         * state to that desired each time this method is called, in
         * particular those states which are not the defaults.
         */
        void drawGlyphVector(Graphics2D graphics, GlyphVector str,
                             int x, int y);
    }

    private static class CharSequenceIterator implements CharacterIterator {
        CharSequence mSequence;
        int mLength;
        int mCurrentIndex;

        CharSequenceIterator() {
        }

        CharSequenceIterator(final CharSequence sequence) {
            initFromCharSequence(sequence);
        }

        void initFromCharSequence(final CharSequence sequence) {
            mSequence = sequence;
            mLength = mSequence.length();
            mCurrentIndex = 0;
        }

        @Override
        public char last() {
            mCurrentIndex = Math.max(0, mLength - 1);
            return current();
        }

        @Override
        public char current() {
            if ((mLength == 0) || (mCurrentIndex >= mLength)) {
                return CharacterIterator.DONE;
            }
            return mSequence.charAt(mCurrentIndex);
        }

        @Override
        public char next() {
            mCurrentIndex++;
            return current();
        }

        @Override
        public char previous() {
            mCurrentIndex = Math.max(mCurrentIndex - 1, 0);
            return current();
        }

        @Override
        public char setIndex(final int position) {
            mCurrentIndex = position;
            return current();
        }

        @Override
        public int getBeginIndex() {
            return 0;
        }

        @Override
        public int getEndIndex() {
            return mLength;
        }

        @Override
        public int getIndex() {
            return mCurrentIndex;
        }

        @Override
        public Object clone() {
            final CharSequenceIterator iter = new CharSequenceIterator(mSequence);
            iter.mCurrentIndex = mCurrentIndex;
            return iter;
        }

        @Override
        public char first() {
            if (mLength == 0) {
                return CharacterIterator.DONE;
            }
            mCurrentIndex = 0;
            return current();
        }
    }

    // Data associated with each rectangle of text
    static class TextData {
        // Back-pointer to String this TextData describes, if it
        // represents a String rather than a single glyph
        private final String str;

        // If this TextData represents a single glyph, this is its
        // unicode ID
        final int unicodeID;

        // The following must be defined and used VERY precisely. This is
        // the offset from the upper-left corner of this rectangle (Java
        // 2D coordinate system) at which the string must be rasterized in
        // order to fit within the rectangle -- the leftmost point of the
        // baseline.
        private final Point origin;

        // This represents the pre-normalized rectangle, which fits
        // within the rectangle on the backing store. We keep a
        // one-pixel border around entries on the backing store to
        // prevent bleeding of adjacent letters when using GL_LINEAR
        // filtering for rendering. The origin of this rectangle is
        // equivalent to the origin above.
        private final Rectangle2D origRect;

        private boolean used; // Whether this text was used recently

        TextData(String str, Point origin, Rectangle2D origRect, int unicodeID) {
            this.str = str;
            this.origin = origin;
            this.origRect = origRect;
            this.unicodeID = unicodeID;
        }

        String string() {
            return str;
        }

        Point origin() {
            return origin;
        }

        // The following three methods are used to locate the glyph
        // within the expanded rectangle coming from normalize()
        int origOriginX() {
            return (int) -origRect.getMinX();
        }

        int origOriginY() {
            return (int) -origRect.getMinY();
        }

        Rectangle2D origRect() {
            return origRect;
        }

        boolean used() {
            return used;
        }

        void markUsed() {
            used = true;
        }

        void clearUsed() {
            used = false;
        }
    }

    class Manager implements BackingStoreManager {
        private Graphics2D g;

        @Override
        public Object allocateBackingStore(final int w, final int h) {
            // FIXME: should consider checking Font's attributes to see
            // whether we're likely to need to support a full RGBA backing
            // store (i.e., non-default Paint, foreground color, etc.), but
            // for now, let's just be more efficient
            return new JhvTextureRenderer(MathUtils.nextPowerOfTwo(w), MathUtils.nextPowerOfTwo(h));
        }

        @Override
        public void deleteBackingStore(final Object backingStore) {
            ((JhvTextureRenderer) backingStore).dispose();
        }

        @Override
        public boolean preExpand(final Rect cause, final int attemptNumber) {
            // Only try this one time; clear out potentially obsolete entries
            // NOTE: this heuristic and the fact that it clears the used bit
            // of all entries seems to cause cycling of entries in some
            // situations, where the backing store becomes small compared to
            // the amount of text on the screen (see the TextFlow demo) and
            // the entries continually cycle in and out of the backing
            // store, decreasing performance. If we added a little age
            // information to the entries, and only cleared out entries
            // above a certain age, this behavior would be eliminated.
            // However, it seems the system usually stabilizes itself, so
            // for now we'll just keep things simple. Note that if we don't
            // clear the used bit here, the backing store tends to increase
            // very quickly to its maximum size, at least with the TextFlow
            // demo when the text is being continually re-laid out.
            if (attemptNumber == 0) {
                if (inBeginEndPair) {
                    // Draw any outstanding glyphs
                    flush();
                }
                clearUnusedEntries();
                return true;
            }
            return false;
        }

        @Override
        public boolean additionFailed(final Rect cause, final int attemptNumber) {
            // Heavy hammer -- might consider doing something different
            packer.clear();
            stringLocations.clear();
            mGlyphProducer.clearAllCacheEntries();
            return attemptNumber == 0;
        }

        @Override
        public boolean canCompact() {
            return true;
        }

        @Override
        public void beginMovement(final Object oldBackingStore, final Object newBackingStore) {
            // Exit the begin / end pair if necessary
            if (inBeginEndPair) {
                // Draw any outstanding glyphs
                flush();
                if (isOrthoMode) {
                    ((JhvTextureRenderer) oldBackingStore).endOrthoRendering();
                } else {
                    ((JhvTextureRenderer) oldBackingStore).end3DRendering();
                }
            }

            final JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;
            g = newRenderer.createGraphics();
        }

        @Override
        public void move(final Object oldBackingStore, final Rect oldLocation,
                         final Object newBackingStore, final Rect newLocation) {
            final JhvTextureRenderer oldRenderer = (JhvTextureRenderer) oldBackingStore;
            final JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;

            if (oldRenderer == newRenderer) {
                // Movement on the same backing store -- easy case
                g.copyArea(oldLocation.x(), oldLocation.y(), oldLocation.w(),
                        oldLocation.h(), newLocation.x() - oldLocation.x(),
                        newLocation.y() - oldLocation.y());
            } else {
                // Need to draw from the old renderer's image into the new one
                final Image img = oldRenderer.getImage();
                g.drawImage(img, newLocation.x(), newLocation.y(),
                        newLocation.x() + newLocation.w(),
                        newLocation.y() + newLocation.h(), oldLocation.x(),
                        oldLocation.y(), oldLocation.x() + oldLocation.w(),
                        oldLocation.y() + oldLocation.h(), null);
            }
        }

        @Override
        public void endMovement(final Object oldBackingStore, final Object newBackingStore) {
            g.dispose();

            // Sync the whole surface
            final JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;
            newRenderer.markDirty(0, 0, newRenderer.getWidth(),
                    newRenderer.getHeight());
            // Re-enter the begin / end pair if necessary
            if (inBeginEndPair) {
                if (isOrthoMode) {
                    ((JhvTextureRenderer) newBackingStore).beginOrthoRendering(beginRenderingWidth,
                            beginRenderingHeight);
                } else {
                    ((JhvTextureRenderer) newBackingStore).begin3DRendering();
                }
            }
        }
    }

    public static class DefaultRenderDelegate implements RenderDelegate {

        @Override
        public Rectangle2D getBounds(final CharSequence str, final Font font,
                                     final FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc,
                    new CharSequenceIterator(str)),
                    frc);
        }

        @Override
        public Rectangle2D getBounds(final String str, final Font font,
                                     final FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc, str), frc);
        }

        @Override
        public Rectangle2D getBounds(final GlyphVector gv, final FontRenderContext frc) {
            return gv.getVisualBounds();
        }

        @Override
        public void drawGlyphVector(final Graphics2D graphics, final GlyphVector str,
                                    final int x, final int y) {
            graphics.drawGlyphVector(str, x, y);
        }

        @Override
        public void draw(final Graphics2D graphics, final String str, final int x, final int y) {
            graphics.drawString(str, x, y);
        }
    }

    //----------------------------------------------------------------------
    // Glyph-by-glyph rendering support
    //

    // A temporary to prevent excessive garbage creation
    final char[] singleUnicode = new char[1];
    final float[] txcArray = new float[12];
    final float[] vtxArray = new float[24];

    /**
     * A Glyph represents either a single unicode glyph or a
     * substring of characters to be drawn. The reason for the dual
     * behavior is so that we can take in a sequence of unicode
     * characters and partition them into runs of individual glyphs,
     * but if we encounter complex text and/or unicode sequences we
     * don't understand, we can render them using the
     * string-by-string method. <P>
     * <p>
     * Glyphs need to be able to re-upload themselves to the backing
     * store on demand as we go along in the render sequence.
     */

    class Glyph {
        // If this Glyph represents an individual unicode glyph, this
        // is its unicode ID. If it represents a String, this is -1.
        private final int unicodeID;
        // If the above field isn't -1, then these fields are used.
        // The glyph code in the font
        private final int glyphCode;
        // The GlyphProducer which created us
        private final GlyphProducer producer;
        // The advance of this glyph
        private final float advance;
        // The GlyphVector for this single character; this is passed
        // in during construction but cleared during the upload
        // process
        private GlyphVector singleUnicodeGlyphVector;
        // The rectangle of this glyph on the backing store, or null
        // if it has been cleared due to space pressure
        private Rect glyphRectForTextureMapping;

        // Creates a Glyph representing an individual Unicode character
        Glyph(final int unicodeID,
              final int glyphCode,
              final float advance,
              final GlyphVector singleUnicodeGlyphVector,
              final GlyphProducer producer) {
            this.unicodeID = unicodeID;
            this.glyphCode = glyphCode;
            this.advance = advance;
            this.singleUnicodeGlyphVector = singleUnicodeGlyphVector;
            this.producer = producer;
        }

        /**
         * Returns this glyph's unicode ID
         */
        int getUnicodeID() {
            return unicodeID;
        }

        /**
         * Returns this glyph's (font-specific) glyph code
         */
        int getGlyphCode() {
            return glyphCode;
        }

        /**
         * Returns the advance for this glyph
         */
        float getAdvance() {
            return advance;
        }

        /**
         * Draws this glyph and returns the (x) advance for this glyph
         */
        float draw3D(final float inX, final float inY, final float z, final float scaleFactor) {
            // This is the code path taken for individual glyphs
            if (glyphRectForTextureMapping == null) {
                upload();
            }

            try {
                if (mPipelinedQuadRenderer == null) {
                    mPipelinedQuadRenderer = new Pipelined_QuadRenderer();
                }

                final JhvTextureRenderer renderer = getBackingStore();
                final Rect rect = glyphRectForTextureMapping;
                final TextData data = (TextData) rect.getUserData();
                data.markUsed();

                final Rectangle2D origRect = data.origRect();

                final float x = inX - (scaleFactor * data.origOriginX());
                final float y = inY - (scaleFactor * ((float) origRect.getHeight() - data.origOriginY()));

                final int texturex = rect.x() + (data.origin().x - data.origOriginX());
                final int texturey = renderer.getHeight() - rect.y() - (int) origRect.getHeight() - (data.origin().y - data.origOriginY());
                final int width = (int) origRect.getWidth();
                final int height = (int) origRect.getHeight();

                final float tx1 = texturex / (float) renderer.getWidth();
                final float ty1 = 1f - texturey / (float) renderer.getHeight();
                final float tx2 = (texturex + width) / (float) renderer.getWidth();
                final float ty2 = 1f - (texturey + height) / (float) renderer.getHeight();

                // A
                txcArray[0] = tx1;
                txcArray[1] = ty1;
                vtxArray[0] = x;
                vtxArray[1] = y;
                vtxArray[2] = z;
                vtxArray[3] = 1;
                // B
                txcArray[2] = tx2;
                txcArray[3] = ty1;
                vtxArray[4] = x + (width * scaleFactor);
                vtxArray[5] = y;
                vtxArray[6] = z;
                vtxArray[7] = 1;
                // C
                txcArray[4] = tx2;
                txcArray[5] = ty2;
                vtxArray[8] = x + (width * scaleFactor);
                vtxArray[9] = y + (height * scaleFactor);
                vtxArray[10] = z;
                vtxArray[11] = 1;
                // A
                txcArray[6] = tx1;
                txcArray[7] = ty1;
                vtxArray[12] = x;
                vtxArray[13] = y;
                vtxArray[14] = z;
                vtxArray[15] = 1;
                // C
                txcArray[8] = tx2;
                txcArray[9] = ty2;
                vtxArray[16] = x + (width * scaleFactor);
                vtxArray[17] = y + (height * scaleFactor);
                vtxArray[18] = z;
                vtxArray[19] = 1;
                // D
                txcArray[10] = tx1;
                txcArray[11] = ty2;
                vtxArray[20] = x;
                vtxArray[21] = y + (height * scaleFactor);
                vtxArray[22] = z;
                vtxArray[23] = 1;

                mPipelinedQuadRenderer.glTexCoord2f(txcArray);
                mPipelinedQuadRenderer.glVertex4f(vtxArray);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            return advance;
        }

        /**
         * Notifies this glyph that it's been cleared out of the cache
         */
        void clear() {
            glyphRectForTextureMapping = null;
        }

        private void upload() {
            final GlyphVector gv = getGlyphVector();
            final Rectangle2D origBBox = preNormalize(renderDelegate.getBounds(gv, getFontRenderContext()));
            final Rectangle2D bbox = normalize(origBBox);
            final Point origin = new Point((int) -bbox.getMinX(),
                    (int) -bbox.getMinY());
            final Rect rect = new Rect(0, 0, (int) bbox.getWidth(),
                    (int) bbox.getHeight(),
                    new TextData(null, origin, origBBox, unicodeID));
            packer.add(rect);
            glyphRectForTextureMapping = rect;
            final Graphics2D g = getGraphics2D();
            // OK, should now have an (x, y) for this rectangle; rasterize
            // the glyph
            final int strx = rect.x() + origin.x;
            final int stry = rect.y() + origin.y;

            // Clear out the area we're going to draw into
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
            g.setComposite(AlphaComposite.Src);

            // Draw the string
            renderDelegate.drawGlyphVector(g, gv, strx, stry);

            if (DRAW_BBOXES) {
                final TextData data = (TextData) rect.getUserData();
                // Draw a bounding box on the backing store
                g.drawRect(strx - data.origOriginX(),
                        stry - data.origOriginY(),
                        (int) data.origRect().getWidth(),
                        (int) data.origRect().getHeight());
                g.drawRect(strx - data.origin().x,
                        stry - data.origin().y,
                        rect.w(),
                        rect.h());
            }

            // Mark this region of the TextureRenderer as dirty
            getBackingStore().markDirty(rect.x(), rect.y(), rect.w(),
                    rect.h());
            // Re-register ourselves with our producer
            producer.register(this);
        }

        private GlyphVector getGlyphVector() {
            final GlyphVector gv = singleUnicodeGlyphVector;
            if (gv != null) {
                singleUnicodeGlyphVector = null; // Don't need this anymore
                return gv;
            }
            singleUnicode[0] = (char) unicodeID;
            return font.createGlyphVector(getFontRenderContext(), singleUnicode);
        }
    }

    class GlyphProducer {
        static final int undefined = -2;
        final FontRenderContext fontRenderContext = null; // FIXME: Never initialized!
        final List<Glyph> glyphsOutput = new ArrayList<>();
        final HashMap<String, GlyphVector> fullGlyphVectorCache = new HashMap<>();
        final HashMap<Character, GlyphMetrics> glyphMetricsCache = new HashMap<>();
        // The mapping from unicode character to font-specific glyph ID
        final int[] unicodes2Glyphs;
        // The mapping from glyph ID to Glyph
        final Glyph[] glyphCache;
        // We re-use this for each incoming string
        final CharSequenceIterator iter = new CharSequenceIterator();

        GlyphProducer(final int fontLengthInGlyphs) {
            unicodes2Glyphs = new int[512];
            glyphCache = new Glyph[fontLengthInGlyphs];
            clearAllCacheEntries();
        }

        List<Glyph> getGlyphs(final CharSequence inString) {
            glyphsOutput.clear();
            GlyphVector fullRunGlyphVector;
            fullRunGlyphVector = fullGlyphVectorCache.get(inString.toString());
            if (fullRunGlyphVector == null) {
                iter.initFromCharSequence(inString);
                fullRunGlyphVector = font.createGlyphVector(getFontRenderContext(), iter);
                fullGlyphVectorCache.put(inString.toString(), fullRunGlyphVector);
            }

            final int lengthInGlyphs = fullRunGlyphVector.getNumGlyphs();
            int i = 0;
            while (i < lengthInGlyphs) {
                final Character letter = CharacterCache.valueOf(inString.charAt(i));
                GlyphMetrics metrics = glyphMetricsCache.get(letter);
                if (metrics == null) {
                    metrics = fullRunGlyphVector.getGlyphMetrics(i);
                    glyphMetricsCache.put(letter, metrics);
                }
                final Glyph glyph = getGlyph(inString, metrics, i);
                if (glyph != null) {
                    glyphsOutput.add(glyph);
                    i++;
                }
            }
            return glyphsOutput;
        }

        void clearCacheEntry(final int unicodeID) {
            final int glyphID = unicodes2Glyphs[unicodeID];
            if (glyphID != undefined) {
                final Glyph glyph = glyphCache[glyphID];
                if (glyph != null) {
                    glyph.clear();
                }
                glyphCache[glyphID] = null;
            }
            unicodes2Glyphs[unicodeID] = undefined;
        }

        void clearAllCacheEntries() {
            for (int i = 0; i < unicodes2Glyphs.length; i++) {
                clearCacheEntry(i);
            }
        }

        void register(final Glyph glyph) {
            unicodes2Glyphs[glyph.getUnicodeID()] = glyph.getGlyphCode();
            glyphCache[glyph.getGlyphCode()] = glyph;
        }

        float getGlyphPixelWidth(final char unicodeID) {
            final Glyph glyph = getGlyph(unicodeID);
            if (glyph != null) {
                return glyph.getAdvance();
            }

            // Have to do this the hard / uncached way
            singleUnicode[0] = unicodeID;
            if (null == fontRenderContext) { // FIXME: Never initialized!
                throw new InternalError("fontRenderContext never initialized!");
            }
            final GlyphVector gv = font.createGlyphVector(fontRenderContext,
                    singleUnicode);
            return gv.getGlyphMetrics(0).getAdvance();
        }

        // Returns a glyph object for this single glyph. Returns null
        // if the unicode or glyph ID would be out of bounds of the
        // glyph cache.
        private Glyph getGlyph(final CharSequence inString,
                               final GlyphMetrics glyphMetrics,
                               final int index) {
            final char unicodeID = inString.charAt(index);

            if (unicodeID >= unicodes2Glyphs.length) {
                return null;
            }

            final int glyphID = unicodes2Glyphs[unicodeID];
            if (glyphID != undefined) {
                return glyphCache[glyphID];
            }

            // Must fabricate the glyph
            singleUnicode[0] = unicodeID;
            final GlyphVector gv = font.createGlyphVector(getFontRenderContext(), singleUnicode);
            return getGlyph(unicodeID, gv, glyphMetrics);
        }

        // It's unclear whether this variant might produce less
        // optimal results than if we can see the entire GlyphVector
        // for the incoming string
        private Glyph getGlyph(final int unicodeID) {
            if (unicodeID >= unicodes2Glyphs.length) {
                return null;
            }

            final int glyphID = unicodes2Glyphs[unicodeID];
            if (glyphID != undefined) {
                return glyphCache[glyphID];
            }
            singleUnicode[0] = (char) unicodeID;
            final GlyphVector gv = font.createGlyphVector(getFontRenderContext(), singleUnicode);
            return getGlyph(unicodeID, gv, gv.getGlyphMetrics(0));
        }

        private Glyph getGlyph(final int unicodeID,
                               final GlyphVector singleUnicodeGlyphVector,
                               final GlyphMetrics metrics) {
            final int glyphCode = singleUnicodeGlyphVector.getGlyphCode(0);
            // Have seen huge glyph codes (65536) coming out of some fonts in some Unicode situations
            if (glyphCode >= glyphCache.length) {
                return null;
            }
            final Glyph glyph = new Glyph(unicodeID,
                    glyphCode,
                    metrics.getAdvance(),
                    singleUnicodeGlyphVector,
                    this);
            register(glyph);
            return glyph;
        }
    }

    private static class CharacterCache {
        private CharacterCache() {
        }

        static final Character cache[] = new Character[127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = (char) i;
            }
        }

        static Character valueOf(final char c) {
            if (c <= 127) { // must cache
                return CharacterCache.cache[c];
            }
            return c;
        }
    }

    static final GLSLTexture glslTexture = new GLSLTexture();
    float[] textColor = {1, 1, 1, 1};

    class Pipelined_QuadRenderer {
        int mOutstandingGlyphsVerticesPipeline = 0;
        final FloatBuffer mTexCoords;
        final FloatBuffer mVertCoords;

        Pipelined_QuadRenderer() {
            mVertCoords = Buffers.newDirectFloatBuffer(kTotalBufferSizeCoordsVerts);
            mTexCoords = Buffers.newDirectFloatBuffer(kTotalBufferSizeCoordsTex);
        }

        void glTexCoord2f(float[] array) {
            mTexCoords.put(array);
        }

        void glVertex4f(float[] array) {
            mVertCoords.put(array);
            mOutstandingGlyphsVerticesPipeline += kVertsPerQuad;
            if (mOutstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts) {
                this.draw();
            }
        }

        void draw() {
            if (mOutstandingGlyphsVerticesPipeline > 0) {
                mVertCoords.rewind();
                mTexCoords.rewind();

                GL2 gl = (GL2) GLContext.getCurrentGL();
                getBackingStore().bind(gl);

                glslTexture.init(gl);
                glslTexture.setData(gl, mVertCoords, mTexCoords);
                glslTexture.render(gl, GL2.GL_TRIANGLES, textColor, mOutstandingGlyphsVerticesPipeline);

                mOutstandingGlyphsVerticesPipeline = 0;
            }
        }
    }

}
