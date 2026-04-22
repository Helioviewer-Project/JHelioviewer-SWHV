package org.helioviewer.jhv.opengl.text;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

// Originally based on JOGL TextRenderer from the Sun/JogAmp codebase.
// This version uses STB rasterization and a fixed glyph atlas per renderer.
public final class TextRenderer {
    private static final int kAtlasWidth = 256;
    private static final int kVertsPerQuad = 6;
    private static final int kQuadsPerBuffer = 100;
    private static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;

    private final float fontSize;
    private final STBTTFontinfo fontInfo;
    private final float scale;
    private final TextureRenderer atlas;
    private final GlyphProducer glyphProducer;
    private final boolean[] loggedMissingGlyphs = new boolean[Character.MAX_VALUE + 1];

    public TextRenderer(float size, ByteBuffer fontData) {
        fontSize = size;
        fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontData)) {
            fontInfo.free();
            throw new IllegalArgumentException("Failed to initialize STB font");
        }
        scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(fontInfo, fontSize);
        glyphProducer = new GlyphProducer();
        atlas = buildAtlas();
    }

    public float getFontSize() {
        return fontSize;
    }

    public void beginRendering(int width, int height) {
        GL.glDisable(GL.DEPTH_TEST);

        Transform.pushProjection();
        Transform.setOrtho2DProjection(0, width, 0, height);
        Transform.pushView();
        Transform.setIdentityView();
    }

    public void begin3DRendering() {}

    public void setColor(float[] color) {
        flush();
        textColor = color;
    }

    public void draw(String str, float x, float y, float z, float scaleFactor) {
        int len = str.length();
        Glyph previousGlyph = null;
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (previousGlyph != null)
                x += getKerning(previousGlyph.getGlyphCode(), glyph.getGlyphCode()) * scaleFactor;
            float advance = glyph.draw3D(x, y, z, scaleFactor);
            x += advance * scaleFactor;
            previousGlyph = glyph;
        }
    }

    public void draw(String str, Matrix4f transform, float scaleFactor) {
        transform.transformPosition(0, 0, 0, transformedOrigin);
        transform.transformPosition(1, 0, 0, transformedBasisX);
        transform.transformPosition(0, 1, 0, transformedBasisY);
        transformedBasisX.sub(transformedOrigin);
        transformedBasisY.sub(transformedOrigin);
        draw(str, transformedOrigin, transformedBasisX, transformedBasisY, scaleFactor);
    }

    public float measureWidth(String str) {
        float width = 0;
        int len = str.length();
        Glyph previousGlyph = null;
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (previousGlyph != null)
                width += getKerning(previousGlyph.getGlyphCode(), glyph.getGlyphCode());
            width += glyph.advance;
            previousGlyph = glyph;
        }
        return width;
    }

    public void draw(String str, Vector3f origin, Vector3f basisX, Vector3f basisY, float scaleFactor) {
        float x = 0;
        int len = str.length();
        Glyph previousGlyph = null;
        for (int i = 0; i < len; ++i) {
            Glyph glyph = glyphProducer.getGlyph(str.charAt(i));
            if (previousGlyph != null)
                x += getKerning(previousGlyph.getGlyphCode(), glyph.getGlyphCode()) * scaleFactor;
            float advance = glyph.draw3D(origin, basisX, basisY, x, 0, scaleFactor);
            x += advance * scaleFactor;
            previousGlyph = glyph;
        }
    }

    public void flush() {
        drawVertices();
    }

    public void endRendering() {
        flush();
        GL.glEnable(GL.DEPTH_TEST);

        Transform.popView();
        Transform.popProjection();
    }

    public void end3DRendering() {
        flush();
    }

    public void dispose() {
        fontInfo.free();
        atlas.dispose();
        glslTexture.dispose();
    }

    private record Bounds(double minX, double minY, double width, double height) {}
    private record TextData(int originX, int originY, int origRectWidth, int origRectHeight, int origRectMinX, int origRectMinY) {
        TextData(int originX, int originY, Bounds origRect) {
            this(originX, originY,
                    (int) origRect.width,
                    (int) origRect.height,
                    (int) -origRect.minX,
                    (int) -origRect.minY);
        }
    }
    private record GlyphRect(int x, int y, int w, int h) {}
    private record StbGlyphData(int glyphIndex, int x0, int y0, int x1, int y1) {
        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }
    }
    private record GlyphLayout(Glyph glyph, TextData data, int rectWidth, int rectHeight) {}

    private Bounds normalize(Bounds src) {
        int boundary = (int) Math.max(1, 0.015 * fontSize);
        return new Bounds((int) Math.floor(src.minX - boundary),
                (int) Math.floor(src.minY - boundary),
                (int) Math.ceil(src.width + 2 * boundary),
                (int) Math.ceil(src.height) + 2 * boundary);
    }

    private TextureRenderer buildAtlas() {
        List<GlyphLayout> layouts = new ArrayList<>(glyphProducer.glyphs.size());
        int atlasWidth = kAtlasWidth;

        for (Glyph glyph : glyphProducer.glyphs) {
            Bounds origBBox = new Bounds(glyph.backendData.x0, glyph.backendData.y0, glyph.backendData.width(), glyph.backendData.height());
            Bounds bbox = normalize(origBBox);
            TextData data = new TextData((int) -bbox.minX, (int) -bbox.minY, origBBox);
            int rectWidth = (int) bbox.width;
            int rectHeight = (int) bbox.height;
            atlasWidth = Math.max(atlasWidth, MathUtils.nextPowerOfTwo(rectWidth));
            layouts.add(new GlyphLayout(glyph, data, rectWidth, rectHeight));
        }

        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (GlyphLayout layout : layouts) {
            if (x > 0 && x + layout.rectWidth > atlasWidth) {
                y += rowHeight;
                x = 0;
                rowHeight = 0;
            }
            layout.glyph.textData = layout.data;
            layout.glyph.glyphRectForTextureMapping = new GlyphRect(x, y, layout.rectWidth, layout.rectHeight);
            x += layout.rectWidth;
            rowHeight = Math.max(rowHeight, layout.rectHeight);
        }

        TextureRenderer renderer = new TextureRenderer(atlasWidth, MathUtils.nextPowerOfTwo(y + rowHeight));
        for (GlyphLayout layout : layouts)
            rasterizeGlyph(renderer, layout.glyph);
        return renderer;
    }

    private @Nullable Glyph createGlyph(char unicodeID) {
        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, unicodeID);
        if (glyphIndex == 0)
            return null;

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
            return new Glyph(glyphIndex, advanceWidth.get(0) * scale, glyphData);
        }
    }

    private void rasterizeGlyph(TextureRenderer renderer, Glyph glyph) {
        GlyphRect rect = glyph.glyphRectForTextureMapping;
        TextData data = glyph.textData;
        StbGlyphData glyphData = glyph.backendData;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ignoredWidth = stack.mallocInt(1);
            IntBuffer ignoredHeight = stack.mallocInt(1);
            IntBuffer ignoredXOffset = stack.mallocInt(1);
            IntBuffer ignoredYOffset = stack.mallocInt(1);
            ByteBuffer bitmap = STBTruetype.stbtt_GetGlyphBitmap(fontInfo, scale, scale, glyphData.glyphIndex(),
                    ignoredWidth, ignoredHeight, ignoredXOffset, ignoredYOffset);
            if (bitmap != null) {
                try {
                    int bitmapX = rect.x + (data.originX - data.origRectMinX);
                    int bitmapY = rect.y + (data.originY - data.origRectMinY);
                    renderer.drawGlyphMask(rect.x, rect.y, rect.w, rect.h, bitmapX, bitmapY, glyphData.width(), glyphData.height(), bitmap);
                } finally {
                    STBTruetype.stbtt_FreeBitmap(bitmap);
                }
            }
        }
    }

    private float getKerning(int leftGlyphCode, int rightGlyphCode) {
        return STBTruetype.stbtt_GetGlyphKernAdvance(fontInfo, leftGlyphCode, rightGlyphCode) * scale;
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
        private static final float epsilon = 0.125f;

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

    private final class Glyph {
        private final int glyphCode;
        private final float advance;
        private final StbGlyphData backendData;
        private GlyphRect glyphRectForTextureMapping;
        private TextData textData;

        Glyph(int glyphCodeValue, float advanceValue, StbGlyphData glyphBackendData) {
            glyphCode = glyphCodeValue;
            advance = advanceValue;
            backendData = glyphBackendData;
        }

        int getGlyphCode() {
            return glyphCode;
        }

        float draw3D(float inX, float inY, float z, float scaleFactor) {
            GlyphRect rect = glyphRectForTextureMapping;
            TextData data = textData;
            int width = data.origRectWidth;
            int height = data.origRectHeight;
            float x = inX - (scaleFactor * data.origRectMinX);
            float y = inY - (scaleFactor * (height - data.origRectMinY));

            int texturex = rect.x + (data.originX - data.origRectMinX);
            int texturey = atlas.getHeight() - rect.y - height - (data.originY - data.origRectMinY);

            float tx1 = texturex / (float) atlas.getWidth();
            float ty1 = 1f - texturey / (float) atlas.getHeight();
            float tx2 = (texturex + width) / (float) atlas.getWidth();
            float ty2 = 1f - (texturey + height) / (float) atlas.getHeight();

            coordPut.put(x, y, z, 1, tx1, ty1);
            coordPut.put(x + (width * scaleFactor), y, z, 1, tx2, ty1);
            coordPut.put(x + (width * scaleFactor), y + (height * scaleFactor), z, 1, tx2, ty2);
            coordPut.put(x, y, z, 1, tx1, ty1);
            coordPut.put(x + (width * scaleFactor), y + (height * scaleFactor), z, 1, tx2, ty2);
            coordPut.put(x, y + (height * scaleFactor), z, 1, tx1, ty2);

            outstandingGlyphsVerticesPipeline += kVertsPerQuad;
            if (outstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts)
                drawVertices();

            return advance;
        }

        float draw3D(Vector3f origin, Vector3f basisX, Vector3f basisY, float inX, float inY, float scaleFactor) {
            GlyphRect rect = glyphRectForTextureMapping;
            TextData data = textData;
            int width = data.origRectWidth;
            int height = data.origRectHeight;
            float x = inX - (scaleFactor * data.origRectMinX);
            float y = inY - (scaleFactor * (height - data.origRectMinY));

            int texturex = rect.x + (data.originX - data.origRectMinX);
            int texturey = atlas.getHeight() - rect.y - height - (data.originY - data.origRectMinY);

            float tx1 = texturex / (float) atlas.getWidth();
            float ty1 = 1f - texturey / (float) atlas.getHeight();
            float tx2 = (texturex + width) / (float) atlas.getWidth();
            float ty2 = 1f - (texturey + height) / (float) atlas.getHeight();

            float x1 = x + (width * scaleFactor);
            float y1 = y + (height * scaleFactor);

            transformedA.set(origin).fma(x, basisX).fma(y, basisY);
            transformedB.set(origin).fma(x1, basisX).fma(y, basisY);
            transformedC.set(origin).fma(x1, basisX).fma(y1, basisY);
            transformedD.set(origin).fma(x, basisX).fma(y1, basisY);

            coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, tx1, ty1);
            coordPut.put(transformedB.x, transformedB.y, transformedB.z, 1, tx2, ty1);
            coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, tx2, ty2);
            coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, tx1, ty1);
            coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, tx2, ty2);
            coordPut.put(transformedD.x, transformedD.y, transformedD.z, 1, tx1, ty2);

            outstandingGlyphsVerticesPipeline += kVertsPerQuad;
            if (outstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts)
                drawVertices();

            return advance;
        }
    }

    private final class GlyphProducer {
        private final Glyph[] glyphsByChar = new Glyph[Character.MAX_VALUE + 1];
        private final List<Glyph> glyphs = new ArrayList<>();
        private final Glyph fallbackGlyph;

        GlyphProducer() {
            String supportedGlyphs = TextFonts.glyphs();
            for (int i = 0; i < supportedGlyphs.length(); i++) {
                char ch = supportedGlyphs.charAt(i);
                Glyph glyph = createGlyph(ch);
                if (glyph != null) {
                    glyphsByChar[ch] = glyph;
                    glyphs.add(glyph);
                }
            }
            fallbackGlyph = glyphsByChar[TextFonts.mapGlyph('\0')];
            if (fallbackGlyph == null)
                throw new IllegalStateException("Fallback glyph '?' is missing from the font");
        }

        Glyph getGlyph(char unicodeID) {
            char mapped = TextFonts.mapGlyph(unicodeID);
            if (mapped != unicodeID)
                logMissingGlyph(unicodeID);
            Glyph glyph = glyphsByChar[mapped];
            return glyph != null ? glyph : fallbackGlyph;
        }
    }

    private void logMissingGlyph(char ch) {
        if (loggedMissingGlyphs[ch])
            return;
        loggedMissingGlyphs[ch] = true;
        String display = Character.isISOControl(ch) ? "" : " '" + ch + "'";
        Log.warn("Missing fixed-atlas glyph" + display + " (U+" + String.format("%04X", (int) ch) + "), using '?'");
    }

    private static final GLSLTexture glslTexture = new GLSLTexture();
    private float[] textColor = Colors.WhiteFloat;

    private int outstandingGlyphsVerticesPipeline = 0;
    private final BufCoord coordBuf = new BufCoord(kTotalBufferSizeVerts);

    private void drawVertices() {
        if (outstandingGlyphsVerticesPipeline > 0) {
            atlas.bind();
            glslTexture.init();
            glslTexture.setCoord(coordBuf);
            glslTexture.renderTexture(GL.TRIANGLES, textColor, 0, outstandingGlyphsVerticesPipeline);
            outstandingGlyphsVerticesPipeline = 0;
        }
    }
}
