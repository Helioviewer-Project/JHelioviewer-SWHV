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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.GLSLTexture;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.packrect.BackingStoreManager;
import com.jogamp.opengl.util.packrect.Rect;
import com.jogamp.opengl.util.packrect.RectanglePacker;

/**
 * Renders bitmapped Java 2D text into an OpenGL window with high
 * performance, full Unicode support, and a simple API. Performs
 * appropriate caching of text rendering results in an OpenGL texture
 * internally to avoid repeated font rasterization. The caching is
 * completely automatic, does not require any user intervention, and
 * has no visible controls in the public API.
 *
 * Using the {@link JhvTextRenderer TextRenderer} is simple. Add a
 * "<code>TextRenderer renderer;</code>" field to your {@link
 * com.jogamp.opengl.GLEventListener GLEventListener}. In your {@link
 * com.jogamp.opengl.GLEventListener#init init} method, add:
 *
 * <PRE>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </PRE>
 *
 * In the {@link com.jogamp.opengl.GLEventListener#display display} method of your
 * {@link com.jogamp.opengl.GLEventListener GLEventListener}, add:
 * <PRE>
 * renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
 * // optionally set the color
 * renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
 * renderer.draw("Text to draw", xPosition, yPosition);
 * // ... more draw commands, color changes, etc.
 * renderer.endRendering();
 * </PRE>
 *
 * Unless you are sharing textures and display lists between OpenGL
 * contexts, you do not need to call the {@link #dispose dispose}
 * method of the TextRenderer; the OpenGL resources it uses
 * internally will be cleaned up automatically when the OpenGL
 * context is destroyed.
 *
 * <b>Note</b> that the TextRenderer may cause the vertex and texture
 * coordinate array buffer bindings to change, or to be unbound. This
 * is important to note if you are using Vertex Buffer Objects (VBOs)
 * in your application.
 *
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
    private static final int kCoordsPerVertVerts = 4;
    private static final int kCoordsPerVertTex = 2;
    private static final int kVertsPerQuad = 6;
    private static final int kQuadsPerBuffer = kVertsPerQuad * 20;
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
    final GlyphProducer glyphProducer;

    private int numRenderCycles;

    // Need to keep track of whether we're in a beginRendering() /
    // endRendering() cycle so we can re-enter the exact same state if
    // we have to reallocate the backing store
    boolean inBeginEndPair;
    boolean isOrthoMode;
    int beginRenderingWidth;
    int beginRenderingHeight;

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
     */
    public JhvTextRenderer(Font font, boolean antialiased, boolean useFractionalMetrics) {
        this.font = font;
        this.antialiased = antialiased;
        this.useFractionalMetrics = useFractionalMetrics;

        // FIXME: consider adjusting the size based on font size
        // (it will already automatically resize if necessary)
        packer = new RectanglePacker(new Manager(), kSize, kSize);
        renderDelegate = new DefaultRenderDelegate();
        glyphProducer = new GlyphProducer(font.getNumGlyphs());
    }

    /**
     * Returns the bounding rectangle of the given String, assuming it
     * was rendered at the origin. See {@link #getBounds(CharSequence)
     * getBounds(CharSequence)}.
     */
    public Rectangle2D getBounds(String str) {
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
    private Rectangle2D getBounds(CharSequence str) {
        // Must return a Rectangle compatible with the layout algorithm - must be idempotent
        return normalize(renderDelegate.getBounds(str, font, getFontRenderContext()));
    }

    // Returns the Font this renderer is using
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
     */
    public void beginRendering(int width, int height) {
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
     */
    public void begin3DRendering() {
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
    public void setColor(float[] color) {
        flush();
        textColor = color;
    }

    /**
     * Draws the supplied String at the desired location using the
     * renderer's current color.
     */
    public void draw(String str, int x, int y) {
        draw3D(str, x, y, 0, 1);
    }

    /**
     * Draws the supplied String at the desired 3D location using the
     * renderer's current color.
     */
    public void draw3D(String str, float x, float y, float z, float scaleFactor) {
        internal_draw3D(str, x, y, z, scaleFactor);
    }

    /**
     * Causes the TextRenderer to flush any internal caches it may be
     * maintaining and draw its rendering results to the screen. This
     * should be called after each call to draw() if you are setting
     * OpenGL state such as the modelview matrix between calls to
     * draw().
     */
    public void flush() {
        drawVertices();
    }

    /**
     * Ends a render cycle with this {@link JhvTextRenderer TextRenderer}.
     * Restores the projection and modelview matrices as well as
     * several OpenGL state bits. Should be paired with {@link
     * #beginRendering beginRendering}.
     */
    public void endRendering() {
        endRendering(true);
    }

    /**
     * Ends a 3D render cycle with this {@link JhvTextRenderer TextRenderer}.
     * Restores several OpenGL state bits. Should be paired with {@link
     * #begin3DRendering begin3DRendering}.
     */
    public void end3DRendering() {
        endRendering(false);
    }

    /**
     * Disposes of all resources this TextRenderer is using. It is not
     * valid to use the TextRenderer after this method is called.
     */
    public void dispose(GL2 gl) {
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

    static Rectangle2D preNormalize(Rectangle2D src) {
        // Need to round to integer coordinates
        // Also give ourselves a little slop around the reported
        // bounds of glyphs because it looks like neither the visual
        // nor the pixel bounds works perfectly well
        int minX = (int) Math.floor(src.getMinX()) - 1;
        int minY = (int) Math.floor(src.getMinY()) - 1;
        int maxX = (int) Math.ceil(src.getMaxX()) + 1;
        int maxY = (int) Math.ceil(src.getMaxY()) + 1;
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    Rectangle2D normalize(Rectangle2D src) {
        // Give ourselves a boundary around each entity on the backing
        // store in order to prevent bleeding of nearby Strings due to
        // the fact that we use linear filtering

        // NOTE that this boundary is quite heuristic and is related
        // to how far away in 3D we may view the text --
        // heuristically, 1.5% of the font's height
        int boundary = (int) Math.max(1, 0.015 * font.getSize());

        return new Rectangle2D.Double((int) Math.floor(src.getMinX() - boundary),
                (int) Math.floor(src.getMinY() - boundary),
                (int) Math.ceil(src.getWidth() + 2 * boundary),
                (int) Math.ceil(src.getHeight()) + 2 * boundary);
    }

    JhvTextureRenderer getBackingStore() {
        JhvTextureRenderer renderer = (JhvTextureRenderer) packer.getBackingStore();
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
        JhvTextureRenderer renderer = getBackingStore();
        if (cachedGraphics == null) {
            cachedGraphics = renderer.createGraphics();
            // Set up composite, font and rendering hints
            cachedGraphics.setComposite(AlphaComposite.Src);
            cachedGraphics.setColor(Color.WHITE);
            cachedGraphics.setFont(font);
            cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
            cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    (useFractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
        }
        return cachedGraphics;
    }

    private void beginRendering(boolean ortho, int width, int height) {
        inBeginEndPair = true;
        isOrthoMode = ortho;
        beginRenderingWidth = width;
        beginRenderingHeight = height;

        internal_beginRendering(ortho, width, height);

        if (!haveMaxSize) {
            // Query OpenGL for the maximum texture size and set it in the
            // RectanglePacker to keep it from expanding too large
            int[] sz = new int[1];
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, sz, 0);
            packer.setMaxSize(sz[0], sz[0]);
            haveMaxSize = true;
        }
    }

    private void endRendering(boolean ortho) {
        flush();

        inBeginEndPair = false;
        internal_endRendering(ortho);

        if (++numRenderCycles >= CYCLES_PER_FLUSH) {
            numRenderCycles = 0;
            clearUnusedEntries();
        }
    }

    static void internal_beginRendering(boolean ortho, int width, int height) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glDisable(GL2.GL_DEPTH_TEST);

            Transform.pushProjection();
            Transform.setOrthoProjection(0, width, 0, height, -1, 1);
            Transform.pushView();
            Transform.setIdentityView();
        }
    }

    static void internal_endRendering(boolean ortho) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glEnable(GL2.GL_DEPTH_TEST);

            Transform.popView();
            Transform.popProjection();
        }
    }

    void clearUnusedEntries() {
        List<Rect> deadRects = new ArrayList<>();

        // Iterate through the contents of the backing store, removing
        // text strings that haven't been used recently
        packer.visit(rect -> {
            TextData data = (TextData) rect.getUserData();
            if (data.used()) {
                data.clearUsed();
            } else {
                deadRects.add(rect);
            }
        });

        for (Rect r : deadRects) {
            packer.remove(r);
            int unicodeToClearFromCache = ((TextData) r.getUserData()).unicodeID;
            if (unicodeToClearFromCache > 0) {
                glyphProducer.clearCacheEntry(unicodeToClearFromCache);
            }
        }

        // If we removed dead rectangles this cycle, try to do a compaction
        if (!deadRects.isEmpty() && packer.verticalFragmentationRatio() > MAX_VERTICAL_FRAGMENTATION) {
            packer.compact();
        }
    }

    private void internal_draw3D(CharSequence str, float x, float y, float z, float scaleFactor) {
        for (Glyph glyph : glyphProducer.getGlyphs(str)) {
            float advance = glyph.draw3D(x, y, z, scaleFactor);
            x += advance * scaleFactor;
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
         * Computes the bounds of the given character sequence relative
         * to the origin.
         */
        Rectangle2D getBounds(CharSequence str, Font font, FontRenderContext frc);

        /**
         * Computes the bounds of the given GlyphVector, already
         * assumed to have been created for a particular Font,
         * relative to the origin.
         */
        Rectangle2D getBounds(GlyphVector gv);

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
        void drawGlyphVector(Graphics2D graphics, GlyphVector str, int x, int y);
    }

    private static class CharSequenceIterator implements CharacterIterator {
        CharSequence mSequence;
        int mLength;
        int mCurrentIndex;

        CharSequenceIterator() {
        }

        CharSequenceIterator(CharSequence sequence) {
            initFromCharSequence(sequence);
        }

        void initFromCharSequence(CharSequence sequence) {
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
        public char setIndex(int position) {
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
        public CharSequenceIterator clone() {
            CharSequenceIterator iter = new CharSequenceIterator(mSequence);
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
        // If this TextData represents a single glyph, this is its unicode ID
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

        TextData(Point origin, Rectangle2D origRect, int unicodeID) {
            this.origin = origin;
            this.origRect = origRect;
            this.unicodeID = unicodeID;
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
        public Object allocateBackingStore(int w, int h) {
            return new JhvTextureRenderer(MathUtils.nextPowerOfTwo(w), MathUtils.nextPowerOfTwo(h));
        }

        @Override
        public void deleteBackingStore(Object backingStore) {
            ((JhvTextureRenderer) backingStore).dispose();
        }

        @Override
        public boolean preExpand(Rect cause, int attemptNumber) {
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
        public boolean additionFailed(Rect cause, int attemptNumber) {
            // Heavy hammer -- might consider doing something different
            packer.clear();
            glyphProducer.clearAllCacheEntries();
            return attemptNumber == 0;
        }

        @Override
        public boolean canCompact() {
            return true;
        }

        @Override
        public void beginMovement(Object oldBackingStore, Object newBackingStore) {
            // Exit the begin / end pair if necessary
            if (inBeginEndPair) {
                // Draw any outstanding glyphs
                flush();
                internal_endRendering(isOrthoMode);
            }

            JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;
            g = newRenderer.createGraphics();
        }

        @Override
        public void move(Object oldBackingStore, Rect oldLocation, Object newBackingStore, Rect newLocation) {
            JhvTextureRenderer oldRenderer = (JhvTextureRenderer) oldBackingStore;
            JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;
            if (oldRenderer == newRenderer) {
                // Movement on the same backing store -- easy case
                g.copyArea(oldLocation.x(), oldLocation.y(), oldLocation.w(),
                        oldLocation.h(), newLocation.x() - oldLocation.x(),
                        newLocation.y() - oldLocation.y());
            } else {
                // Need to draw from the old renderer's image into the new one
                Image img = oldRenderer.getImage();
                g.drawImage(img, newLocation.x(), newLocation.y(),
                        newLocation.x() + newLocation.w(),
                        newLocation.y() + newLocation.h(), oldLocation.x(),
                        oldLocation.y(), oldLocation.x() + oldLocation.w(),
                        oldLocation.y() + oldLocation.h(), null);
            }
        }

        @Override
        public void endMovement(Object oldBackingStore, Object newBackingStore) {
            g.dispose();

            // Sync the whole surface
            JhvTextureRenderer newRenderer = (JhvTextureRenderer) newBackingStore;
            newRenderer.markDirty(0, 0, newRenderer.getWidth(), newRenderer.getHeight());
            // Re-enter the begin / end pair if necessary
            if (inBeginEndPair) {
                internal_beginRendering(isOrthoMode, beginRenderingWidth, beginRenderingHeight);
            }
        }
    }

    private static class DefaultRenderDelegate implements RenderDelegate {

        @Override
        public Rectangle2D getBounds(CharSequence str, Font font, FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc, new CharSequenceIterator(str)));
        }

        @Override
        public Rectangle2D getBounds(GlyphVector gv) {
            return gv.getVisualBounds();
        }

        @Override
        public void drawGlyphVector(Graphics2D graphics, GlyphVector str, int x, int y) {
            graphics.drawGlyphVector(str, x, y);
        }

    }

    // Glyph-by-glyph rendering support

    // A temporary to prevent excessive garbage creation
    final char[] singleUnicode = new char[1];
    final float[] texArray = new float[kVertsPerQuad * 2];
    final float[] vertArray = new float[kVertsPerQuad * 4];

    /**
     * A Glyph represents either a single unicode glyph or a
     * substring of characters to be drawn. The reason for the dual
     * behavior is so that we can take in a sequence of unicode
     * characters and partition them into runs of individual glyphs,
     * but if we encounter complex text and/or unicode sequences we
     * don't understand, we can render them using the
     * string-by-string method.
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
        Glyph(int unicodeID, int glyphCode, float advance, GlyphVector singleUnicodeGlyphVector, GlyphProducer producer) {
            this.unicodeID = unicodeID;
            this.glyphCode = glyphCode;
            this.advance = advance;
            this.singleUnicodeGlyphVector = singleUnicodeGlyphVector;
            this.producer = producer;
        }

        // Returns this glyph's unicode ID
        int getUnicodeID() {
            return unicodeID;
        }

        // Returns this glyph's (font-specific) glyph code
        int getGlyphCode() {
            return glyphCode;
        }

        // Draws this glyph and returns the (x) advance for this glyph
        float draw3D(float inX, float inY, float z, float scaleFactor) {
            // This is the code path taken for individual glyphs
            if (glyphRectForTextureMapping == null) {
                upload();
            }

            JhvTextureRenderer renderer = getBackingStore();
            Rect rect = glyphRectForTextureMapping;
            TextData data = (TextData) rect.getUserData();
            data.markUsed();

            Rectangle2D origRect = data.origRect();

            float x = inX - (scaleFactor * data.origOriginX());
            float y = inY - (scaleFactor * ((float) origRect.getHeight() - data.origOriginY()));

            int texturex = rect.x() + (data.origin().x - data.origOriginX());
            int texturey = renderer.getHeight() - rect.y() - (int) origRect.getHeight() - (data.origin().y - data.origOriginY());
            int width = (int) origRect.getWidth();
            int height = (int) origRect.getHeight();

            float tx1 = texturex / (float) renderer.getWidth();
            float ty1 = 1f - texturey / (float) renderer.getHeight();
            float tx2 = (texturex + width) / (float) renderer.getWidth();
            float ty2 = 1f - (texturey + height) / (float) renderer.getHeight();

            // A
            texArray[0] = tx1;
            texArray[1] = ty1;
            vertArray[0] = x;
            vertArray[1] = y;
            vertArray[2] = z;
            vertArray[3] = 1;
            // B
            texArray[2] = tx2;
            texArray[3] = ty1;
            vertArray[4] = x + (width * scaleFactor);
            vertArray[5] = y;
            vertArray[6] = z;
            vertArray[7] = 1;
            // C
            texArray[4] = tx2;
            texArray[5] = ty2;
            vertArray[8] = x + (width * scaleFactor);
            vertArray[9] = y + (height * scaleFactor);
            vertArray[10] = z;
            vertArray[11] = 1;
            // A
            texArray[6] = tx1;
            texArray[7] = ty1;
            vertArray[12] = x;
            vertArray[13] = y;
            vertArray[14] = z;
            vertArray[15] = 1;
            // C
            texArray[8] = tx2;
            texArray[9] = ty2;
            vertArray[16] = x + (width * scaleFactor);
            vertArray[17] = y + (height * scaleFactor);
            vertArray[18] = z;
            vertArray[19] = 1;
            // D
            texArray[10] = tx1;
            texArray[11] = ty2;
            vertArray[20] = x;
            vertArray[21] = y + (height * scaleFactor);
            vertArray[22] = z;
            vertArray[23] = 1;

            pushVertices(texArray, vertArray);

            return advance;
        }

        // Notifies this glyph that it's been cleared out of the cache
        void clear() {
            glyphRectForTextureMapping = null;
        }

        private void upload() {
            GlyphVector gv = getGlyphVector();
            Rectangle2D origBBox = preNormalize(renderDelegate.getBounds(gv));
            Rectangle2D bbox = normalize(origBBox);
            Point origin = new Point((int) -bbox.getMinX(), (int) -bbox.getMinY());
            Rect rect = new Rect(0, 0, (int) bbox.getWidth(), (int) bbox.getHeight(), new TextData(origin, origBBox, unicodeID));
            packer.add(rect);
            glyphRectForTextureMapping = rect;
            Graphics2D g = getGraphics2D();
            // OK, should now have an (x, y) for this rectangle; rasterize
            // the glyph
            int strx = rect.x() + origin.x;
            int stry = rect.y() + origin.y;

            // Clear out the area we're going to draw into
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
            g.setComposite(AlphaComposite.Src);

            // Draw the string
            renderDelegate.drawGlyphVector(g, gv, strx, stry);

            if (DRAW_BBOXES) {
                TextData data = (TextData) rect.getUserData();
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
            getBackingStore().markDirty(rect.x(), rect.y(), rect.w(), rect.h());
            // Re-register ourselves with our producer
            producer.register(this);
        }

        private GlyphVector getGlyphVector() {
            GlyphVector gv = singleUnicodeGlyphVector;
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
        final List<Glyph> glyphsOutput = new ArrayList<>();
        final HashMap<String, GlyphVector> fullGlyphVectorCache = new HashMap<>();
        final HashMap<Character, GlyphMetrics> glyphMetricsCache = new HashMap<>();
        // The mapping from unicode character to font-specific glyph ID
        final int[] unicodes2Glyphs;
        // The mapping from glyph ID to Glyph
        final Glyph[] glyphCache;
        // We re-use this for each incoming string
        final CharSequenceIterator iter = new CharSequenceIterator();

        GlyphProducer(int fontLengthInGlyphs) {
            unicodes2Glyphs = new int[512];
            glyphCache = new Glyph[fontLengthInGlyphs];
            clearAllCacheEntries();
        }

        List<Glyph> getGlyphs(CharSequence inString) {
            glyphsOutput.clear();
            GlyphVector fullRunGlyphVector;
            fullRunGlyphVector = fullGlyphVectorCache.get(inString.toString());
            if (fullRunGlyphVector == null) {
                iter.initFromCharSequence(inString);
                fullRunGlyphVector = font.createGlyphVector(getFontRenderContext(), iter);
                fullGlyphVectorCache.put(inString.toString(), fullRunGlyphVector);
            }

            int lengthInGlyphs = fullRunGlyphVector.getNumGlyphs();
            int i = 0;
            while (i < lengthInGlyphs) {
                Character letter = CharacterCache.valueOf(inString.charAt(i));
                GlyphMetrics metrics = glyphMetricsCache.get(letter);
                if (metrics == null) {
                    metrics = fullRunGlyphVector.getGlyphMetrics(i);
                    glyphMetricsCache.put(letter, metrics);
                }
                Glyph glyph = getGlyph(inString, metrics, i);
                if (glyph != null) {
                    glyphsOutput.add(glyph);
                    i++;
                }
            }
            return glyphsOutput;
        }

        void clearCacheEntry(int unicodeID) {
            int glyphID = unicodes2Glyphs[unicodeID];
            if (glyphID != undefined) {
                Glyph glyph = glyphCache[glyphID];
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

        void register(Glyph glyph) {
            unicodes2Glyphs[glyph.getUnicodeID()] = glyph.getGlyphCode();
            glyphCache[glyph.getGlyphCode()] = glyph;
        }

        // Returns a glyph object for this single glyph. Returns null
        // if the unicode or glyph ID would be out of bounds of the
        // glyph cache.
        @Nullable
        private Glyph getGlyph(CharSequence inString, GlyphMetrics glyphMetrics, int index) {
            char unicodeID = inString.charAt(index);
            if (unicodeID >= unicodes2Glyphs.length) {
                return null;
            }

            int glyphID = unicodes2Glyphs[unicodeID];
            if (glyphID != undefined) {
                return glyphCache[glyphID];
            }

            // Must fabricate the glyph
            singleUnicode[0] = unicodeID;
            GlyphVector gv = font.createGlyphVector(getFontRenderContext(), singleUnicode);
            return getGlyph(unicodeID, gv, glyphMetrics);
        }

        @Nullable
        private Glyph getGlyph(int unicodeID, GlyphVector singleUnicodeGlyphVector, GlyphMetrics metrics) {
            int glyphCode = singleUnicodeGlyphVector.getGlyphCode(0);
            // Have seen huge glyph codes (65536) coming out of some fonts in some Unicode situations
            if (glyphCode >= glyphCache.length) {
                return null;
            }
            Glyph glyph = new Glyph(unicodeID, glyphCode, metrics.getAdvance(), singleUnicodeGlyphVector, this);
            register(glyph);
            return glyph;
        }
    }

    private static class CharacterCache {

        static final Character cache[] = new Character[127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = (char) i;
            }
        }

        static Character valueOf(char c) {
            if (c <= 127) { // must cache
                return cache[c];
            }
            return c;
        }
    }

    private static final GLSLTexture glslTexture = new GLSLTexture();
    private float[] textColor = BufferUtils.colorWhiteFloat;

    private int outstandingGlyphsVerticesPipeline = 0;
    private final FloatBuffer texCoords = BufferUtils.newFloatBuffer(kTotalBufferSizeCoordsTex);
    private final FloatBuffer vertCoords = BufferUtils.newFloatBuffer(kTotalBufferSizeCoordsVerts);

    void pushVertices(float[] _texArray, float[] _vertArray) {
        texCoords.put(_texArray);
        vertCoords.put(_vertArray);
        outstandingGlyphsVerticesPipeline += kVertsPerQuad;
        if (outstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts) {
            drawVertices();
        }
    }

    private void drawVertices() {
        if (outstandingGlyphsVerticesPipeline > 0) {
            vertCoords.rewind();
            texCoords.rewind();

            GL2 gl = (GL2) GLContext.getCurrentGL();
            getBackingStore().bind(gl);

            glslTexture.init(gl);
            glslTexture.setData(gl, vertCoords, texCoords);
            glslTexture.render(gl, GL2.GL_TRIANGLES, textColor, outstandingGlyphsVerticesPipeline);
            outstandingGlyphsVerticesPipeline = 0;
        }
    }

}
