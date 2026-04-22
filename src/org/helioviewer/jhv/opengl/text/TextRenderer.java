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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLTexture;
import org.helioviewer.jhv.opengl.text.packrect.BackingStoreManager;
import org.helioviewer.jhv.opengl.text.packrect.Rect;
import org.helioviewer.jhv.opengl.text.packrect.RectanglePacker;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Renders bitmapped text into an OpenGL window with high performance, full
 * Unicode support, and a simple API. Glyphs are rasterized through STB and
 * cached in an OpenGL texture atlas to avoid repeated rasterization. The
 * caching is automatic, does not require user intervention, and has no visible
 * controls in the public API.
 * <p>
 * Using the {@link TextRenderer TextRenderer} is simple. Add a
 * "<code>TextRenderer renderer;</code>" field to your rendering code.
 * During initialization, add:
 *
 * <PRE>
 * renderer = new TextRenderer(...);
 * </PRE>
 * <p>
 * During a render pass, add:
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
 * <p>
 * <b>Note</b> that the TextRenderer may cause the vertex and texture
 * coordinate array buffer bindings to change, or to be unbound. This
 * is important to note if you are using Vertex Buffer Objects (VBOs)
 * in your application.
 * <p>
 * Internally, the renderer uses a rectangle packing algorithm to
 * pack glyph bitmaps into a larger OpenGL texture. The internal backing
 * store is maintained by an internal texture renderer.
 *
 * @author John Burkey
 * @author Kenneth Russell
 */
public class TextRenderer {
    private static final int kSize = 256;
    private static final int kVertsPerQuad = 6;
    private static final int kQuadsPerBuffer = 100;
    private static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
    private final float fontSize;
    private RectanglePacker packer;
    private boolean haveMaxSize;
    private final StbTextBackend textBackend;
    private final GlyphProducer glyphProducer;

    // Need to keep track of whether we're in a beginRendering() /
    // endRendering() cycle so we can re-enter the exact same state if
    // we have to reallocate the backing store
    private boolean inBeginEndPair;
    private boolean isOrthoMode;
    private int beginRenderingWidth;
    private int beginRenderingHeight;

    /**
     * Creates a new TextRenderer using the supplied font bytes and pixel size.
     *
     * @param size     the pixel size of the font
     * @param fontData the font data to rasterize glyphs from
     */
    public TextRenderer(float size, ByteBuffer fontData) {
        fontSize = size;

        // FIXME: consider adjusting the size based on font size
        // (it will already automatically resize if necessary)
        packer = new RectanglePacker(new Manager(), kSize, kSize);
        textBackend = new StbTextBackend(fontData, fontSize);
        glyphProducer = new GlyphProducer();
    }

    public float getFontSize() {
        return fontSize;
    }

    /**
     * Begins rendering with this {@link TextRenderer TextRenderer}
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
     * Begins rendering of 2D text in 3D with this {@link TextRenderer
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
     * color channels, although premultiplied colors are used
     * internally. The default color is opaque white.
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
        Glyph previousGlyph = null;
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (glyph != null) {
                if (previousGlyph != null)
                    x += textBackend.getKerning(previousGlyph.getGlyphCode(), glyph.getGlyphCode()) * scaleFactor;
                float advance = glyph.draw3D(x, y, z, scaleFactor);
                x += advance * scaleFactor;
                previousGlyph = glyph;
            }
        }
    }

    public void draw3D(String str, Matrix4f transform, float scaleFactor) {
        // The matrix places the local text frame in 3D. Transform the local origin and the unit x/y axes once,
        // then build all glyph corners from that transformed basis instead of re-transforming each corner.
        transform.transformPosition(0, 0, 0, transformedOrigin);
        transform.transformPosition(1, 0, 0, transformedBasisX);
        transform.transformPosition(0, 1, 0, transformedBasisY);
        transformedBasisX.sub(transformedOrigin);
        transformedBasisY.sub(transformedOrigin);
        draw3D(str, transformedOrigin, transformedBasisX, transformedBasisY, scaleFactor);
    }

    public void draw3D(String str, Vector3f origin, Vector3f basisX, Vector3f basisY, float scaleFactor) {
        float x = 0;
        int len = str.length();
        Glyph previousGlyph = null;
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (glyph != null) {
                if (previousGlyph != null)
                    x += textBackend.getKerning(previousGlyph.getGlyphCode(), glyph.getGlyphCode()) * scaleFactor;
                float advance = glyph.draw3D(origin, basisX, basisY, x, 0, scaleFactor);
                x += advance * scaleFactor;
                previousGlyph = glyph;
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
     * Ends a render cycle with this {@link TextRenderer TextRenderer}.
     * Restores the projection and modelview matrices as well as
     * several OpenGL state bits. Should be paired with {@link
     * #beginRendering beginRendering}.
     */
    public void endRendering() {
        endRendering(true);
    }

    /**
     * Ends a 3D render cycle with this {@link TextRenderer TextRenderer}.
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
    public void dispose() {
        packer.dispose();
        packer = null;
        textBackend.dispose();
        glslTexture.dispose();
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private record Bounds(double minX, double minY, double width, double height) {
        double maxX() {
            return minX + width;
        }

        double maxY() {
            return minY + height;
        }
    }

    private static Bounds preNormalize(Bounds src) {
        // Need to round to integer coordinates
        // Also give ourselves a little slop around the reported
        // bounds of glyphs because it looks like neither the visual
        // nor the pixel bounds works perfectly well
        int minX = (int) Math.floor(src.minX) - 1;
        int minY = (int) Math.floor(src.minY) - 1;
        int maxX = (int) Math.ceil(src.maxX()) + 1;
        int maxY = (int) Math.ceil(src.maxY()) + 1;
        return new Bounds(minX, minY, maxX - minX, maxY - minY);
    }

    private Bounds normalize(Bounds src) {
        // Give ourselves a boundary around each entity on the backing
        // store in order to prevent bleeding of nearby Strings due to
        // the fact that we use linear filtering

        // NOTE that this boundary is quite heuristic and is related
        // to how far away in 3D we may view the text --
        // heuristically, 1.5% of the font's height
        int boundary = (int) Math.max(1, 0.015 * fontSize);

        return new Bounds((int) Math.floor(src.minX - boundary),
                (int) Math.floor(src.minY - boundary),
                (int) Math.ceil(src.width + 2 * boundary),
                (int) Math.ceil(src.height) + 2 * boundary);
    }

    private TextureRenderer getBackingStore() {
        return (TextureRenderer) packer.getBackingStore();
    }

    private void beginRendering(boolean ortho, int width, int height) {
        inBeginEndPair = true;
        isOrthoMode = ortho;
        beginRenderingWidth = width;
        beginRenderingHeight = height;

        internal_beginRendering(ortho, width, height);

        if (!haveMaxSize) {
            // Set the maximum texture size in the RectanglePacker to keep it from expanding too large
            packer.setMaxSize(GL.maxTextureSize, GL.maxTextureSize);
            haveMaxSize = true;
        }
    }

    private void endRendering(boolean ortho) {
        flush();

        inBeginEndPair = false;
        internal_endRendering(ortho);
    }

    private void internal_beginRendering(boolean ortho, int width, int height) {
        if (ortho) {
            GL.glDisable(GL.DEPTH_TEST);

            Transform.pushProjection();
            Transform.setOrtho2DProjection(0, width, 0, height);
            Transform.pushView();
            Transform.setIdentityView();
        }
    }

    private void internal_endRendering(boolean ortho) {
        if (ortho) {
            GL.glEnable(GL.DEPTH_TEST);

            Transform.popView();
            Transform.popProjection();
        }
    }
    // Data associated with each rectangle of text
    private static record TextData(
            int originX,
            int originY,
            int origRectWidth,
            int origRectHeight,
            int origRectMinX,
            int origRectMinY) {
        // The following must be defined and used VERY precisely. This is
        // the offset from the upper-left corner of this rectangle (Java
        // 2D coordinate system) at which the string must be rasterized in
        // order to fit within the rectangle -- the leftmost point of the
        // baseline.
        TextData(int originX, int originY, Bounds origRect) {
            this(originX, originY,
                    (int) origRect.width,
                    (int) origRect.height,
                    (int) -origRect.minX,
                    (int) -origRect.minY);
        }

        // The following three methods are used to locate the glyph
        // within the expanded rectangle coming from normalize()
        int origOriginX() {
            return origRectMinX;
        }

        int origOriginY() {
            return origRectMinY;
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

    private final class Manager implements BackingStoreManager {

        @Override
        public Object allocateBackingStore(int w, int h) {
            return new TextureRenderer(MathUtils.nextPowerOfTwo(w), MathUtils.nextPowerOfTwo(h));
        }

        @Override
        public void deleteBackingStore(Object backingStore) {
            ((TextureRenderer) backingStore).dispose();
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
        }

        @Override
        public void move(Object oldBackingStore, Rect oldLocation, Object newBackingStore, Rect newLocation) {
            TextureRenderer oldRenderer = (TextureRenderer) oldBackingStore;
            TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
            if (oldRenderer == newRenderer) {
                // Movement on the same backing store -- easy case
                newRenderer.copyArea(oldLocation.x(), oldLocation.y(), oldLocation.w(),
                        oldLocation.h(), newLocation.x() - oldLocation.x(),
                        newLocation.y() - oldLocation.y());
            } else {
                newRenderer.copyFrom(oldRenderer, oldLocation.x(), oldLocation.y(), oldLocation.w(), oldLocation.h(), newLocation.x(), newLocation.y());
            }
        }

        @Override
        public void endMovement(Object oldBackingStore, Object newBackingStore) {
            // Sync the whole surface
            TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
            newRenderer.markDirty(0, 0, newRenderer.getWidth(), newRenderer.getHeight());
            // Re-enter the begin / end pair if necessary
            if (inBeginEndPair) {
                internal_beginRendering(isOrthoMode, beginRenderingWidth, beginRenderingHeight);
            }
        }
    }

    private final class StbTextBackend {
        private final STBTTFontinfo fontInfo;
        private final float scale;

        StbTextBackend(ByteBuffer sourceFontData, float pixelHeight) {
            fontInfo = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(fontInfo, sourceFontData)) {
                fontInfo.free();
                throw new IllegalArgumentException("Failed to initialize STB font");
            }
            scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(fontInfo, pixelHeight);
        }

        private @Nullable Glyph createGlyph(char unicodeID) {
            int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, unicodeID);
            if (glyphIndex == 0) {
                return null;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer advanceWidth = stack.mallocInt(1);
                IntBuffer ignoredLeftBearing = stack.mallocInt(1);
                IntBuffer x0 = stack.mallocInt(1);
                IntBuffer y0 = stack.mallocInt(1);
                IntBuffer x1 = stack.mallocInt(1);
                IntBuffer y1 = stack.mallocInt(1);

                STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, ignoredLeftBearing);
                STBTruetype.stbtt_GetGlyphBitmapBox(fontInfo, glyphIndex, scale, scale, x0, y0, x1, y1);

                StbGlyphData glyphData = new StbGlyphData(glyphIndex, x0.get(0), y0.get(0), x1.get(0), y1.get(0));
                return new Glyph(unicodeID, glyphIndex, advanceWidth.get(0) * scale, glyphData);
            }
        }

        private void uploadGlyph(Glyph glyph) {
            StbGlyphData glyphData = (StbGlyphData) glyph.backendData;
            Bounds origBBox = new Bounds(glyphData.x0, glyphData.y0, glyphData.width(), glyphData.height());
            Bounds bbox = normalize(origBBox);
            int originX = (int) -bbox.minX;
            int originY = (int) -bbox.minY;
            Rect rect = new Rect(0, 0, (int) bbox.width, (int) bbox.height, new TextData(originX, originY, origBBox));
            packer.add(rect);
            glyph.glyphRectForTextureMapping = rect;

            TextureRenderer renderer = getBackingStore();
            TextData data = (TextData) rect.getUserData();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ignoredWidth = stack.mallocInt(1);
                IntBuffer ignoredHeight = stack.mallocInt(1);
                IntBuffer ignoredXOffset = stack.mallocInt(1);
                IntBuffer ignoredYOffset = stack.mallocInt(1);
                ByteBuffer bitmap = STBTruetype.stbtt_GetGlyphBitmap(fontInfo, scale, scale, glyphData.glyphIndex(),
                        ignoredWidth, ignoredHeight, ignoredXOffset, ignoredYOffset);
                if (bitmap != null) {
                    try {
                        int bitmapX = rect.x() + (data.originX() - data.origOriginX());
                        int bitmapY = rect.y() + (data.originY() - data.origOriginY());
                        renderer.drawGlyphMask(rect, bitmapX, bitmapY, glyphData.width(), glyphData.height(), bitmap);
                    } finally {
                        STBTruetype.stbtt_FreeBitmap(bitmap);
                    }
                }
            }
        }

        private float getKerning(int leftGlyphCode, int rightGlyphCode) {
            return STBTruetype.stbtt_GetGlyphKernAdvance(fontInfo, leftGlyphCode, rightGlyphCode) * scale;
        }

        private void dispose() {
            fontInfo.free();
        }
    }

    private record StbGlyphData(int glyphIndex, int x0, int y0, int x1, int y1) {
        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }
    }

    private interface CoordPut {

        void put(float x, float y, float z, float w, float c0, float c1);

    }

    private final class DirectPut implements CoordPut {

        @Override
        public void put(float x, float y, float z, float w, float c0, float c1) {
            coordBuf.putCoord(x, y, z, w, c0, c1);
        }

    }

    private final class SurfacePut implements CoordPut {

        private static final float epsilon = 0.125f; // should depend on triangle size

        @Override
        public void put(float x, float y, float z, float w, float c0, float c1) {
            float n = 1 - x * x - y * y;
            coordBuf.putCoord(x, y, n > 0 ? epsilon + (float) Math.sqrt(n) : epsilon, w, c0, c1);
        }

    }

    private final CoordPut directPut = new DirectPut();
    private final CoordPut surfacePut = new SurfacePut();

    private final Vector3f transformedOrigin = new Vector3f();
    private final Vector3f transformedBasisX = new Vector3f();
    private final Vector3f transformedBasisY = new Vector3f();
    private final Vector3f transformedA = new Vector3f();
    private final Vector3f transformedB = new Vector3f();
    private final Vector3f transformedC = new Vector3f();
    private final Vector3f transformedD = new Vector3f();

    private CoordPut coordPut = directPut;

    public void setDirectPut() {
        coordPut = directPut;
    }

    public void setSurfacePut() {
        coordPut = surfacePut;
    }

    // Glyph-by-glyph rendering support

    /**
     * A Glyph represents a single unicode glyph and knows how to upload itself
     * to the backing store on demand.
     */

    private final class Glyph {
        // This glyph's unicode ID.
        private final int unicodeID;
        // The glyph code in the font.
        private final int glyphCode;
        // The advance of this glyph.
        private final float advance;
        // Backend-specific glyph data used for rasterization.
        private final Object backendData;
        // The rectangle of this glyph on the backing store, or null
        // if it has been cleared due to space pressure
        private Rect glyphRectForTextureMapping;

        // Creates a Glyph representing an individual Unicode character
        Glyph(int unicodeIDValue, int glyphCodeValue, float advanceValue, Object glyphBackendData) {
            unicodeID = unicodeIDValue;
            glyphCode = glyphCodeValue;
            advance = advanceValue;
            backendData = glyphBackendData;
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

            TextureRenderer renderer = getBackingStore();
            Rect rect = glyphRectForTextureMapping;
            TextData data = (TextData) rect.getUserData();
            //data.markUsed();

            int width = data.origRectWidth();
            int height = data.origRectHeight();
            float x = inX - (scaleFactor * data.origOriginX());
            float y = inY - (scaleFactor * (height - data.origOriginY()));

            int texturex = rect.x() + (data.originX() - data.origOriginX());
            int texturey = renderer.getHeight() - rect.y() - height - (data.originY() - data.origOriginY());

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

        float draw3D(Vector3f origin, Vector3f basisX, Vector3f basisY, float inX, float inY, float scaleFactor) {
            if (glyphRectForTextureMapping == null) {
                upload();
            }

            TextureRenderer renderer = getBackingStore();
            Rect rect = glyphRectForTextureMapping;
            TextData data = (TextData) rect.getUserData();

            int width = data.origRectWidth();
            int height = data.origRectHeight();
            float x = inX - (scaleFactor * data.origOriginX());
            float y = inY - (scaleFactor * (height - data.origOriginY()));

            int texturex = rect.x() + (data.originX() - data.origOriginX());
            int texturey = renderer.getHeight() - rect.y() - height - (data.originY() - data.origOriginY());

            float tx1 = texturex / (float) renderer.getWidth();
            float ty1 = 1f - texturey / (float) renderer.getHeight();
            float tx2 = (texturex + width) / (float) renderer.getWidth();
            float ty2 = 1f - (texturey + height) / (float) renderer.getHeight();

            float x1 = x + (width * scaleFactor);
            float y1 = y + (height * scaleFactor);

            transformedA.set(origin).fma(x, basisX).fma(y, basisY);
            transformedB.set(origin).fma(x1, basisX).fma(y, basisY);
            transformedC.set(origin).fma(x1, basisX).fma(y1, basisY);
            transformedD.set(origin).fma(x, basisX).fma(y1, basisY);

            coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, tx1, ty1); // A
            coordPut.put(transformedB.x, transformedB.y, transformedB.z, 1, tx2, ty1); // B
            coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, tx2, ty2); // C
            coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, tx1, ty1); // A
            coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, tx2, ty2); // C
            coordPut.put(transformedD.x, transformedD.y, transformedD.z, 1, tx1, ty2); // D

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
            textBackend.uploadGlyph(this);
        }

    }

    private final class GlyphProducer {
        private static final int undefined = -2;
        // The mapping from unicode character to font-specific glyph ID
        private final int[] unicodes2Glyphs;
        // The mapping from glyph ID to Glyph
        private final Glyph[] glyphCache;

        GlyphProducer() {
            unicodes2Glyphs = new int[10000]; // highest character we can draw
            glyphCache = new Glyph[65536];
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

        // Returns a glyph object for this single glyph. Returns null if the
        // unicode or glyph ID would be out of bounds of the glyph cache.
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
            Glyph glyph = textBackend.createGlyph(unicodeID);
            if (glyph == null) {
                return null;
            }
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
            getBackingStore().bind();

            glslTexture.init();
            glslTexture.setCoord(coordBuf);
            glslTexture.renderTexture(GL.TRIANGLES, textColor, 0, outstandingGlyphsVerticesPipeline);
            outstandingGlyphsVerticesPipeline = 0;
        }
    }

}
