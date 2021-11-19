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
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.text.StringCharacterIterator;
//import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.GLInfo;
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
 * <p>
 * Using the {@link JhvTextRenderer TextRenderer} is simple. Add a
 * "<code>TextRenderer renderer;</code>" field to your {@link
 * com.jogamp.opengl.GLEventListener GLEventListener}. In your {@link
 * com.jogamp.opengl.GLEventListener#init init} method, add:
 *
 * <PRE>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </PRE>
 * <p>
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
 * <p>
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
    //private static final int CYCLES_PER_FLUSH = 100;

    // The amount of vertical dead space on the backing store before we
    // force a compaction
    //private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;
    private static final int kVertsPerQuad = 6;
    private static final int kQuadsPerBuffer = 100;
    private static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
    private final Font font;
    private final boolean antialiased;
    private final boolean useFractionalMetrics;

    private RectanglePacker packer;
    private boolean haveMaxSize;
    private final RenderDelegate renderDelegate;
    private JhvTextureRenderer cachedBackingStore;
    private Graphics2D cachedGraphics;
    private FontRenderContext cachedFontRenderContext;
    private final GlyphProducer glyphProducer;

    //private int numRenderCycles;

    // Need to keep track of whether we're in a beginRendering() /
    // endRendering() cycle so we can re-enter the exact same state if
    // we have to reallocate the backing store
    private boolean inBeginEndPair;
    private boolean isOrthoMode;
    private int beginRenderingWidth;
    private int beginRenderingHeight;

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
     * Returns the bounding rectangle of the given String,
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
    public Rectangle2D getBounds(String str) {
        // Must return a Rectangle compatible with the layout algorithm - must be idempotent
        return normalize(renderDelegate.getBounds(font.createGlyphVector(getFontRenderContext(), new StringCharacterIterator(str))));
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
    private FontRenderContext getFontRenderContext() {
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
     * @param width  the width of the current on-screen OpenGL drawable
     * @param height the height of the current on-screen OpenGL drawable
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
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (glyph != null) {
                float advance = glyph.draw3D(x, y, z, scaleFactor);
                x += advance * scaleFactor;
            }
        }
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

    private static Rectangle2D preNormalize(Rectangle2D src) {
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

    private Rectangle2D normalize(Rectangle2D src) {
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

    private JhvTextureRenderer getBackingStore() {
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

    private Graphics2D getGraphics2D() {
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
            // Set the maximum texture size in the RectanglePacker to keep it from expanding too large
            packer.setMaxSize(GLInfo.maxTextureSize, GLInfo.maxTextureSize);
            haveMaxSize = true;
        }
    }

    private void endRendering(boolean ortho) {
        flush();

        inBeginEndPair = false;
        internal_endRendering(ortho);
/*
        if (++numRenderCycles >= CYCLES_PER_FLUSH) {
            numRenderCycles = 0;
            clearUnusedEntries();
        }
*/
    }

    private static void internal_beginRendering(boolean ortho, int width, int height) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glDisable(GL2.GL_DEPTH_TEST);

            Transform.pushProjection();
            Transform.setOrthoProjection(0, width, 0, height, -1, 1);
            Transform.pushView();
            Transform.setIdentityView();
        }
    }

    private static void internal_endRendering(boolean ortho) {
        if (ortho) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            gl.glEnable(GL2.GL_DEPTH_TEST);

            Transform.popView();
            Transform.popProjection();
        }
    }
/*
    void clearUnusedEntries() {
        ArrayList<Rect> deadRects = new ArrayList<>();
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

        deadRects.forEach(rect -> {
            packer.remove(rect);
            int unicodeToClearFromCache = ((TextData) rect.getUserData()).unicodeID;
            if (unicodeToClearFromCache > 0) {
                glyphProducer.clearCacheEntry(unicodeToClearFromCache);
            }
        });

        // If we removed dead rectangles this cycle, try to do a compaction
        if (!deadRects.isEmpty() && packer.verticalFragmentationRatio() > MAX_VERTICAL_FRAGMENTATION) {
            packer.compact();
        }
    }
*/

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

    // Data associated with each rectangle of text
    static class TextData {
        // If this TextData represents a single glyph, this is its unicode ID
        //final int unicodeID;

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
        //private final Rectangle2D origRect;
        private final int origRectWidth;
        private final int origRectHeight;
        private final int origRectMinX;
        private final int origRectMinY;

        //private boolean used; // Whether this text was used recently

        TextData(Point _origin, Rectangle2D origRect, int _unicodeID) {
            //unicodeID = _unicodeID;
            origin = _origin;
            origRectWidth = (int) origRect.getWidth();
            origRectHeight = (int) origRect.getHeight();
            origRectMinX = (int) -origRect.getMinX();
            origRectMinY = (int) -origRect.getMinY();
        }

        Point origin() {
            return origin;
        }

        // The following three methods are used to locate the glyph
        // within the expanded rectangle coming from normalize()
        int origOriginX() {
            return origRectMinX;
        }

        int origOriginY() {
            return origRectMinY;
        }

        int origRectWidth() {
            return origRectWidth;
        }

        int origRectHeight() {
            return origRectHeight;
        }
/*
        boolean used() {
            return used;
        }

        void markUsed() {
            used = true;
        }

        void clearUsed() {
            used = false;
        }
*/
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
                //clearUnusedEntries();
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
        public Rectangle2D getBounds(GlyphVector gv) {
            return gv.getVisualBounds();
        }

        @Override
        public void drawGlyphVector(Graphics2D graphics, GlyphVector str, int x, int y) {
            graphics.drawGlyphVector(str, x, y);
        }

    }

    private interface CoordPut {

        void put(float x, float y, float z, float w, float c0, float c1);

    }

    private class DirectPut implements CoordPut {

        @Override
        public void put(float x, float y, float z, float w, float c0, float c1) {
            coordBuf.putCoord(x, y, z, w, c0, c1);
        }

    }

    private class SurfacePut implements CoordPut {

        private static final float epsilon = 0.125f; // should depend on triangle size

        @Override
        public void put(float x, float y, float z, float w, float c0, float c1) {
            float n = 1 - x * x - y * y;
            coordBuf.putCoord(x, y, n > 0 ? epsilon + (float) Math.sqrt(n) : epsilon, w, c0, c1);
        }

    }

    private final CoordPut directPut = new DirectPut();
    private final CoordPut surfacePut = new SurfacePut();

    private CoordPut coordPut = directPut;

    public void setDirectPut() {
        coordPut = directPut;
    }

    public void setSurfacePut() {
        coordPut = surfacePut;
    }

    // Glyph-by-glyph rendering support

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
        // The advance of this glyph
        private final float advance;
        // The GlyphVector for this single character
        private final GlyphVector singleUnicodeGlyphVector;
        // The rectangle of this glyph on the backing store, or null
        // if it has been cleared due to space pressure
        private Rect glyphRectForTextureMapping;

        // Creates a Glyph representing an individual Unicode character
        Glyph(int unicodeID, int glyphCode, float advance, GlyphVector singleUnicodeGlyphVector) {
            this.unicodeID = unicodeID;
            this.glyphCode = glyphCode;
            this.advance = advance;
            this.singleUnicodeGlyphVector = singleUnicodeGlyphVector;
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
            //data.markUsed();

            int width = data.origRectWidth();
            int height = data.origRectHeight();
            float x = inX - (scaleFactor * data.origOriginX());
            float y = inY - (scaleFactor * (height - data.origOriginY()));

            int texturex = rect.x() + (data.origin().x - data.origOriginX());
            int texturey = renderer.getHeight() - rect.y() - height - (data.origin().y - data.origOriginY());

            float tx1 = texturex / (float) renderer.getWidth();
            float ty1 = 1f - texturey / (float) renderer.getHeight();
            float tx2 = (texturex + width) / (float) renderer.getWidth();
            float ty2 = 1f - (texturey + height) / (float) renderer.getHeight();

            coordPut.put(x, y, z, 1, tx1, ty1); // A
            coordPut.put(x + (width * scaleFactor), y, z, 1, tx2, ty1); // B
            coordPut.put(x + (width * scaleFactor), y + (height * scaleFactor), z, 1, tx2, ty2); // C
            coordPut.put(x, y, z, 1, tx1, ty1); // A
            coordPut.put(x + (width * scaleFactor), y + (height * scaleFactor), z, 1, tx2, ty2); // C
            coordPut.put(x, y + (height * scaleFactor), z, 1, tx1, ty2); // D

            outstandingGlyphsVerticesPipeline += kVertsPerQuad;
            if (outstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts) {
                drawVertices();
            }

            return advance;
        }

        // Notifies this glyph that it's been cleared out of the cache
        void clear() {
            glyphRectForTextureMapping = null;
        }

        private void upload() {
            GlyphVector gv = singleUnicodeGlyphVector;
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
                        data.origRectWidth(),
                        data.origRectHeight());
                g.drawRect(strx - data.origin().x,
                        stry - data.origin().y,
                        rect.w(),
                        rect.h());
            }
            // Mark this region of the TextureRenderer as dirty
            getBackingStore().markDirty(rect.x(), rect.y(), rect.w(), rect.h());
        }

    }

    class GlyphProducer {
        // A temporary to prevent excessive garbage creation
        private final char[] singleUnicode = new char[1];
        private static final int undefined = -2;
        // The mapping from unicode character to font-specific glyph ID
        private final int[] unicodes2Glyphs;
        // The mapping from glyph ID to Glyph
        private final Glyph[] glyphCache;

        GlyphProducer(int fontLengthInGlyphs) {
            unicodes2Glyphs = new int[10000]; // highest character we can draw
            glyphCache = new Glyph[fontLengthInGlyphs];
            clearAllCacheEntries();
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

        private void register(Glyph glyph) {
            unicodes2Glyphs[glyph.getUnicodeID()] = glyph.getGlyphCode();
            glyphCache[glyph.getGlyphCode()] = glyph;
        }

        // Returns a glyph object for this single glyph. Returns null
        // if the unicode or glyph ID would be out of bounds of the
        // glyph cache.
        @Nullable
        Glyph getGlyph(char unicodeID) {
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
            int gc = gv.getGlyphCode(0);
            // Have seen huge glyph codes (65536) coming out of some fonts in some Unicode situations
            if (gc >= glyphCache.length) {
                return null;
            }
            Glyph glyph = new Glyph(unicodeID, gc, gv.getGlyphMetrics(0).getAdvance(), gv);
            register(glyph);
            return glyph;
        }
    }

    private static final GLSLTexture glslTexture = new GLSLTexture();
    private float[] textColor = Colors.WhiteFloat;

    private int outstandingGlyphsVerticesPipeline = 0;
    private final BufCoord coordBuf = new BufCoord(kTotalBufferSizeVerts);

    private void drawVertices() {
        if (outstandingGlyphsVerticesPipeline > 0) {
            GL2 gl = (GL2) GLContext.getCurrentGL();
            getBackingStore().bind(gl);

            glslTexture.init(gl);
            glslTexture.setCoord(gl, coordBuf);
            glslTexture.renderTexture(gl, GL2.GL_TRIANGLES, textColor, 0, outstandingGlyphsVerticesPipeline);
            outstandingGlyphsVerticesPipeline = 0;
        }
    }

}
