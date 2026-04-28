package org.helioviewer.jhv.opengl.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.opengl.BufCoord;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLTexture;
import org.helioviewer.jhv.opengl.GLTexture;

import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class MsdfTextRenderer {
    private static final String ATLAS_IMAGE = "/msdf/atlas-128.png";
    private static final String ATLAS_JSON = "/msdf/atlas-128.json";
    private static final String ATLAS_CHARSET = "/msdf/charset";

    private static final int kVertsPerQuad = 6;
    private static final int kQuadsPerBuffer = 100;
    private static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
    private static final float SURFACE_EPSILON = 0.03f;

    private final AtlasTexture texture;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Long, Float> kerning = new HashMap<>();
    private final Set<Integer> missingGlyphs = new HashSet<>();
    private final float fontSize;
    private final float unitRangeX;
    private final float unitRangeY;

    private final GLSLTexture glslTexture = new GLSLTexture();
    private float[] textColor = Colors.WhiteFloat;
    private int outstandingGlyphsVerticesPipeline;
    private final BufCoord coordBuf = new BufCoord(kTotalBufferSizeVerts);

    private final Vector3f transformedA = new Vector3f();
    private final Vector3f transformedB = new Vector3f();
    private final Vector3f transformedC = new Vector3f();
    private final Vector3f transformedD = new Vector3f();

    private CoordPut coordPut = this::putDirect;

    public MsdfTextRenderer() {
        try {
            Atlas atlas = loadAtlas();
            texture = new AtlasTexture(ATLAS_IMAGE, atlas.width, atlas.height);
            fontSize = atlas.size;
            unitRangeX = atlas.distanceRange / atlas.width;
            unitRangeY = atlas.distanceRange / atlas.height;
            validateCharset();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load MSDF atlas", e);
        }
    }

    public float getFontSize() {
        return fontSize;
    }

    public float measureWidth(String str) {
        float width = 0;
        Glyph previousGlyph = null;
        int len = str.length();
        for (int offset = 0; offset < len;) {
            int codePoint = str.codePointAt(offset);
            Glyph glyph = getGlyph(codePoint);
            if (glyph != null) {
                if (previousGlyph != null)
                    width += getKerning(previousGlyph.codePoint, glyph.codePoint);
                width += glyph.advance;
                previousGlyph = glyph;
            }
            offset += Character.charCount(codePoint);
        }
        return width;
    }

    public void beginRendering(int width, int height) {
        beginRendering(true, width, height);
    }

    public void begin3DRendering() {
        beginRendering(false, 0, 0);
    }

    public void setColor(float[] color) {
        flush();
        textColor = color;
    }

    public void draw(String str, float x, float y, float z, float scaleFactor) {
        int len = str.length();
        Glyph previousGlyph = null;
        for (int offset = 0; offset < len;) {
            int codePoint = str.codePointAt(offset);
            Glyph glyph = getGlyph(codePoint);
            if (glyph != null) {
                if (previousGlyph != null)
                    x += getKerning(previousGlyph.codePoint, glyph.codePoint) * scaleFactor;
                float advance = glyph.draw3D(x, y, z, scaleFactor);
                x += advance * scaleFactor;
                previousGlyph = glyph;
            }
            offset += Character.charCount(codePoint);
        }
    }

    public void draw(String str, Vector3f origin, Vector3f basisX, Vector3f basisY, float scaleFactor) {
        float x = 0;
        int len = str.length();
        Glyph previousGlyph = null;
        for (int offset = 0; offset < len;) {
            int codePoint = str.codePointAt(offset);
            Glyph glyph = getGlyph(codePoint);
            if (glyph != null) {
                if (previousGlyph != null)
                    x += getKerning(previousGlyph.codePoint, glyph.codePoint) * scaleFactor;
                float advance = glyph.draw3D(origin, basisX, basisY, x, 0, scaleFactor);
                x += advance * scaleFactor;
                previousGlyph = glyph;
            }
            offset += Character.charCount(codePoint);
        }
    }

    public void flush() {
        drawVertices();
    }

    public void endRendering() {
        endRendering(true);
    }

    public void end3DRendering() {
        endRendering(false);
    }

    public void dispose() {
        texture.dispose();
        glslTexture.dispose();
    }

    public void setDirectPut() {
        coordPut = this::putDirect;
    }

    public void setSurfacePut() {
        coordPut = this::putSurface;
    }

    private void beginRendering(boolean ortho, int width, int height) {
        internal_beginRendering(ortho, width, height);
    }

    private void endRendering(boolean ortho) {
        flush();
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

    private Atlas loadAtlas() throws IOException {
        JSONObject json;
        try (InputStream input = FileUtils.getResource(ATLAS_JSON)) {
            json = JSONUtils.get(input);
        }

        JSONObject atlasJson = json.getJSONObject("atlas");
        int width = atlasJson.getInt("width");
        int height = atlasJson.getInt("height");
        float distanceRange = atlasJson.getFloat("distanceRange");
        float size = atlasJson.getFloat("size");

        JSONArray glyphArray = json.getJSONArray("glyphs");
        for (int i = 0; i < glyphArray.length(); i++) {
            JSONObject glyphJson = glyphArray.getJSONObject(i);
            int codePoint = glyphJson.getInt("unicode");
            float advance = glyphJson.getFloat("advance") * size;
            JSONObject planeBounds = glyphJson.optJSONObject("planeBounds");
            JSONObject atlasBounds = glyphJson.optJSONObject("atlasBounds");
            Glyph glyph = planeBounds == null || atlasBounds == null
                    ? new Glyph(codePoint, advance)
                    : new Glyph(codePoint, advance, size, width, height, planeBounds, atlasBounds);
            glyphs.put(codePoint, glyph);
        }

        JSONArray kerningArray = json.optJSONArray("kerning");
        if (kerningArray != null) {
            for (int i = 0; i < kerningArray.length(); i++) {
                JSONObject kerningJson = kerningArray.getJSONObject(i);
                int left = kerningJson.getInt("unicode1");
                int right = kerningJson.getInt("unicode2");
                float advance = kerningJson.getFloat("advance") * size;
                kerning.put(kerningKey(left, right), advance);
            }
        }

        return new Atlas(width, height, distanceRange, size);
    }

    private void validateCharset() throws IOException {
        String charset;
        try (InputStream input = FileUtils.getResource(ATLAS_CHARSET)) {
            charset = FileUtils.streamToString(input).trim();
        }
        if (charset.length() >= 2 && charset.charAt(0) == '"' && charset.charAt(charset.length() - 1) == '"')
            charset = charset.substring(1, charset.length() - 1);

        int len = charset.length();
        for (int offset = 0; offset < len;) {
            int codePoint = charset.codePointAt(offset);
            if (!glyphs.containsKey(codePoint))
                Log.warn("MSDF charset glyph absent from atlas: " + describeCodePoint(codePoint));
            offset += Character.charCount(codePoint);
        }
    }

    private Glyph getGlyph(int codePoint) {
        Glyph glyph = glyphs.get(codePoint);
        if (glyph == null && missingGlyphs.add(codePoint))
            Log.warn("MSDF glyph absent from atlas: " + describeCodePoint(codePoint));
        return glyph;
    }

    private static String describeCodePoint(int codePoint) {
        return "'" + new String(Character.toChars(codePoint)) + "' (U+" + Integer.toHexString(codePoint).toUpperCase() + ")";
    }

    private float getKerning(int leftCodePoint, int rightCodePoint) {
        return kerning.getOrDefault(kerningKey(leftCodePoint, rightCodePoint), 0f);
    }

    private static long kerningKey(int leftCodePoint, int rightCodePoint) {
        return ((long) leftCodePoint << Integer.SIZE) | Integer.toUnsignedLong(rightCodePoint);
    }

    private void putDirect(float x, float y, float z, float w, float c0, float c1) {
        coordBuf.putCoord(x, y, z, w, c0, c1);
    }

    private void putSurface(float x, float y, float z, float w, float c0, float c1) {
        float n = 1 - x * x - y * y;
        if (n > 0) {
            float scale = 1 + SURFACE_EPSILON;
            float zSurface = (float) Math.sqrt(n);
            coordBuf.putCoord(x * scale, y * scale, zSurface * scale, w, c0, c1);
        } else {
            coordBuf.putCoord(x, y, SURFACE_EPSILON, w, c0, c1);
        }
    }

    private void drawVertices() {
        if (outstandingGlyphsVerticesPipeline > 0) {
            texture.bind();

            glslTexture.init();
            glslTexture.setCoord(coordBuf);
            glslTexture.renderMsdfTexture(GL.TRIANGLES, textColor, unitRangeX, unitRangeY, 0, outstandingGlyphsVerticesPipeline);
            outstandingGlyphsVerticesPipeline = 0;
        }
    }

    private interface CoordPut {
        void put(float x, float y, float z, float w, float c0, float c1);
    }

    private record Atlas(int width, int height, float distanceRange, float size) {}

    private final class Glyph {
        private final int codePoint;
        private final float advance;
        private final boolean visible;
        private final float x0;
        private final float y0;
        private final float x1;
        private final float y1;
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;

        private Glyph(int glyphCodePoint, float glyphAdvance) {
            codePoint = glyphCodePoint;
            advance = glyphAdvance;
            visible = false;
            x0 = 0;
            y0 = 0;
            x1 = 0;
            y1 = 0;
            u0 = 0;
            v0 = 0;
            u1 = 0;
            v1 = 0;
        }

        private Glyph(int glyphCodePoint, float glyphAdvance, float size, int atlasWidth, int atlasHeight,
                JSONObject planeBounds, JSONObject atlasBounds) {
            codePoint = glyphCodePoint;
            advance = glyphAdvance;
            visible = true;
            x0 = planeBounds.getFloat("left") * size;
            y0 = planeBounds.getFloat("bottom") * size;
            x1 = planeBounds.getFloat("right") * size;
            y1 = planeBounds.getFloat("top") * size;
            u0 = atlasBounds.getFloat("left") / atlasWidth;
            u1 = atlasBounds.getFloat("right") / atlasWidth;
            v0 = 1f - atlasBounds.getFloat("bottom") / atlasHeight;
            v1 = 1f - atlasBounds.getFloat("top") / atlasHeight;
        }

        private float draw3D(float inX, float inY, float z, float scaleFactor) {
            if (visible) {
                float xLeft = inX + x0 * scaleFactor;
                float xRight = inX + x1 * scaleFactor;
                float yBottom = inY + y0 * scaleFactor;
                float yTop = inY + y1 * scaleFactor;

                coordPut.put(xLeft, yBottom, z, 1, u0, v0); // A
                coordPut.put(xRight, yBottom, z, 1, u1, v0); // B
                coordPut.put(xRight, yTop, z, 1, u1, v1); // C
                coordPut.put(xLeft, yBottom, z, 1, u0, v0); // A
                coordPut.put(xRight, yTop, z, 1, u1, v1); // C
                coordPut.put(xLeft, yTop, z, 1, u0, v1); // D
                glyphQueued();
            }
            return advance;
        }

        private float draw3D(Vector3f origin, Vector3f basisX, Vector3f basisY, float inX, float inY, float scaleFactor) {
            if (visible) {
                float xLeft = inX + x0 * scaleFactor;
                float xRight = inX + x1 * scaleFactor;
                float yBottom = inY + y0 * scaleFactor;
                float yTop = inY + y1 * scaleFactor;

                transformedA.set(origin).fma(xLeft, basisX).fma(yBottom, basisY);
                transformedB.set(origin).fma(xRight, basisX).fma(yBottom, basisY);
                transformedC.set(origin).fma(xRight, basisX).fma(yTop, basisY);
                transformedD.set(origin).fma(xLeft, basisX).fma(yTop, basisY);

                coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, u0, v0); // A
                coordPut.put(transformedB.x, transformedB.y, transformedB.z, 1, u1, v0); // B
                coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, u1, v1); // C
                coordPut.put(transformedA.x, transformedA.y, transformedA.z, 1, u0, v0); // A
                coordPut.put(transformedC.x, transformedC.y, transformedC.z, 1, u1, v1); // C
                coordPut.put(transformedD.x, transformedD.y, transformedD.z, 1, u0, v1); // D
                glyphQueued();
            }
            return advance;
        }
    }

    private void glyphQueued() {
        outstandingGlyphsVerticesPipeline += kVertsPerQuad;
        if (outstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts)
            drawVertices();
    }

    private static final class AtlasTexture {
        private final GLTexture texture = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);

        AtlasTexture(String resource, int expectedWidth, int expectedHeight) throws IOException {
            ByteBuffer encoded;
            try (InputStream input = FileUtils.getResource(resource)) {
                byte[] bytes = input.readAllBytes();
                encoded = MemoryUtil.memAlloc(bytes.length);
                encoded.put(bytes).flip();
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer ignoredChannels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, ignoredChannels, 3);
                if (pixels == null)
                    throw new IOException("Failed to decode " + resource + ": " + STBImage.stbi_failure_reason());

                try {
                    upload(resource, expectedWidth, expectedHeight, width.get(0), height.get(0), pixels);
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            } finally {
                MemoryUtil.memFree(encoded);
            }
        }

        private void upload(String resource, int expectedWidth, int expectedHeight, int actualWidth, int actualHeight, ByteBuffer pixels) throws IOException {
            if (actualWidth != expectedWidth || actualHeight != expectedHeight) {
                throw new IOException(resource + " dimensions " + actualWidth + "x" + actualHeight + " do not match atlas JSON "
                        + expectedWidth + "x" + expectedHeight);
            }

            texture.bind();
            GL.glPixelStorei(GL.UNPACK_ALIGNMENT, 1);
            GL.glPixelStorei(GL.UNPACK_ROW_LENGTH, expectedWidth);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_BASE_LEVEL, 0);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 15);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE);
            GL.glTexParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE);
            GL.glTexImage2D(GL.TEXTURE_2D, 0, GL.RGB8, expectedWidth, expectedHeight, 0, GL.RGB, GL.UNSIGNED_BYTE, pixels);
            GL.glGenerateMipmap(GL.TEXTURE_2D);
        }

        private void bind() {
            texture.bind();
        }

        private void dispose() {
            texture.delete();
        }
    }
}
